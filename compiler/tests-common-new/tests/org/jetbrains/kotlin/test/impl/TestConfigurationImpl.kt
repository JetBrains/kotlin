/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.ComposedDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.ModuleStructureExtractorImpl
import org.jetbrains.kotlin.test.utils.TestDisposable

@OptIn(TestInfrastructureInternals::class)
class TestConfigurationImpl(
    testInfo: KotlinTestInfo,

    defaultsProvider: DefaultsProvider,
    assertions: AssertionsService,

    facades: List<Constructor<AbstractTestFacade<*, *>>>,

    analysisHandlers: List<Constructor<AnalysisHandler<*>>>,

    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<EnvironmentConfigurator>>,

    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    moduleStructureTransformers: List<ModuleStructureTransformer>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,

    compilerConfigurationProvider: ((Disposable, List<EnvironmentConfigurator>) -> CompilerConfigurationProvider)?,

    override val metaInfoHandlerEnabled: Boolean,

    directives: List<DirectivesContainer>,
    override val defaultRegisteredDirectives: RegisteredDirectives,
    additionalServices: List<ServiceRegistrationData>
) : TestConfiguration() {
    override val rootDisposable: Disposable = TestDisposable()
    override val testServices: TestServices = TestServices()

    init {
        testServices.register(KotlinTestInfo::class, testInfo)
        additionalServices.forEach { testServices.register(it) }
    }

    private val allDirectives = directives.toMutableSet()
    override val directives: DirectivesContainer by lazy {
        when (allDirectives.size) {
            0 -> DirectivesContainer.Empty
            1 -> allDirectives.single()
            else -> ComposedDirectivesContainer(allDirectives)
        }
    }

    private val environmentConfigurators: List<EnvironmentConfigurator> =
        environmentConfigurators.map { it.invoke(testServices) }.also { configurators ->
            configurators.flatMapTo(allDirectives) { it.directivesContainers }
            for (configurator in configurators) {
                configurator.additionalServices.forEach { testServices.register(it) }
            }
        }

    override val moduleStructureExtractor: ModuleStructureExtractor = ModuleStructureExtractorImpl(
        testServices,
        additionalSourceProviders.map { it.invoke(testServices) }.also {
            it.flatMapTo(allDirectives) { provider -> provider.directives }
        },
        moduleStructureTransformers,
        this.environmentConfigurators
    )

    override val metaTestConfigurators: List<MetaTestConfigurator> = metaTestConfigurators.map {
        it.invoke(testServices).also { configurator ->
            allDirectives += configurator.directives
        }
    }

    override val afterAnalysisCheckers: List<AfterAnalysisChecker> = afterAnalysisCheckers.map {
        it.invoke(testServices).also { checker ->
            allDirectives += checker.directives
        }
    }

    init {
        testServices.apply {
            @OptIn(ExperimentalStdlibApi::class)
            val sourceFilePreprocessors = sourcePreprocessors.map { it.invoke(this@apply) }
            val sourceFileProvider = SourceFileProviderImpl(this, sourceFilePreprocessors)
            register(SourceFileProvider::class, sourceFileProvider)

            val environmentProvider =
                compilerConfigurationProvider?.invoke(rootDisposable, this@TestConfigurationImpl.environmentConfigurators)
                    ?: CompilerConfigurationProviderImpl(rootDisposable, this@TestConfigurationImpl.environmentConfigurators)
            register(CompilerConfigurationProvider::class, environmentProvider)

            register(AssertionsService::class, assertions)
            register(DefaultsProvider::class, defaultsProvider)

            register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(defaultRegisteredDirectives))

            val metaInfoProcessors = additionalMetaInfoProcessors.map { it.invoke(this) }
            register(GlobalMetadataInfoHandler::class, GlobalMetadataInfoHandler(this, metaInfoProcessors))
        }
    }

    private val facades: Map<TestArtifactKind<*>, Map<TestArtifactKind<*>, AbstractTestFacade<*, *>>> =
        facades
            .map { it.invoke(testServices) }
            .groupBy { it.inputKind }
            .mapValues { (frontendKind, converters) ->
                converters.groupBy { it.outputKind }.mapValues {
                    it.value.singleOrNull() ?: manyFacadesError("converters", "$frontendKind -> ${it.key}")
                }
            }


    private val analysisHandlers: Map<TestArtifactKind<*>, List<AnalysisHandler<*>>> =
        analysisHandlers.map { it.invoke(testServices).also(this::registerDirectivesAndServices) }
            .groupBy { it.artifactKind }
            .withDefault { emptyList() }

    private fun manyFacadesError(name: String, kinds: String): Nothing {
        error("Too many $name passed for $kinds configuration")
    }

    private fun registerDirectivesAndServices(handler: AnalysisHandler<*>) {
        allDirectives += handler.directivesContainers
        testServices.register(handler.additionalServices)
    }

    init {
        testServices.apply {
            this@TestConfigurationImpl.facades.values.forEach {
                it.values.forEach { facade ->
                    register(facade.additionalServices)
                    allDirectives += facade.additionalDirectives
                }
            }
        }
    }

    override fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> getFacade(
        inputKind: TestArtifactKind<I>,
        outputKind: TestArtifactKind<O>
    ): AbstractTestFacade<I, O> {
        @Suppress("UNCHECKED_CAST")
        return facades[inputKind]?.get(outputKind) as AbstractTestFacade<I, O>?
            ?: facadeNotFoundError(inputKind, outputKind)
    }

    private fun facadeNotFoundError(from: Any, to: Any): Nothing {
        error("Facade for converting '$from' to '$to' not found")
    }

    override fun <A : ResultingArtifact<A>> getHandlers(artifactKind: TestArtifactKind<A>): List<AnalysisHandler<A>> {
        @Suppress("UNCHECKED_CAST")
        return analysisHandlers.getValue(artifactKind) as List<AnalysisHandler<A>>
    }

    override fun getAllHandlers(): List<AnalysisHandler<*>> {
        return analysisHandlers.values.flatten()
    }
}
