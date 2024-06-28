/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
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

    steps: List<TestStepBuilder<*, *>>,

    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>,

    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>,
    moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,

    compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?,
    runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>,

    override val metaInfoHandlerEnabled: Boolean,

    directives: List<DirectivesContainer>,
    override val defaultRegisteredDirectives: RegisteredDirectives,
    override val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>,
    additionalServices: List<ServiceRegistrationData>,

    val originalBuilder: TestConfigurationBuilder.ReadOnlyBuilder
) : TestConfiguration(), TestService {
    override val rootDisposable: Disposable = TestDisposable("${TestConfigurationImpl::class.simpleName}.rootDisposable")
    override val testServices: TestServices = TestServices()

    init {
        testServices.register(TestConfigurationImpl::class, this)
        testServices.register(KotlinTestInfo::class, testInfo)
        val runtimeClassPathProviders = runtimeClasspathProviders.map { it.invoke(testServices) }
        testServices.register(RuntimeClasspathProvidersContainer::class, RuntimeClasspathProvidersContainer(runtimeClassPathProviders))
        additionalServices.forEach {
            testServices.register(it, skipAlreadyRegistered = false)
        }
    }

    private val allDirectives = directives.toMutableSet()
    override val directives: DirectivesContainer by lazy {
        when (allDirectives.size) {
            0 -> DirectivesContainer.Empty
            1 -> allDirectives.single()
            else -> ComposedDirectivesContainer(allDirectives)
        }
    }

    private val environmentConfigurators: List<AbstractEnvironmentConfigurator> =
        environmentConfigurators
            .map { it.invoke(testServices) }
            .also { it.registerDirectivesAndServices() }

    override val preAnalysisHandlers: List<PreAnalysisHandler> =
        preAnalysisHandlers.map { it.invoke(testServices) }

    override val moduleStructureExtractor: ModuleStructureExtractor = ModuleStructureExtractorImpl(
        testServices,
        additionalSourceProviders
            .map { it.invoke(testServices) }
            .also { it.registerDirectivesAndServices() },
        moduleStructureTransformers.map { it(testServices) },
        this.environmentConfigurators
    )

    override val metaTestConfigurators: List<MetaTestConfigurator> = metaTestConfigurators.map { constructor ->
        constructor.invoke(testServices).also { it.registerDirectivesAndServices() }
    }

    init {
        testServices.apply {
            register(
                EnvironmentConfiguratorsProvider::class,
                EnvironmentConfiguratorsProvider(this@TestConfigurationImpl.environmentConfigurators)
            )
            val sourceFilePreprocessors = sourcePreprocessors.map { it.invoke(this@apply) }
            val sourceFileProvider = SourceFileProviderImpl(this, sourceFilePreprocessors)
            register(SourceFileProvider::class, sourceFileProvider)

            val environmentProvider =
                compilerConfigurationProvider?.invoke(this, rootDisposable, this@TestConfigurationImpl.environmentConfigurators)
                    ?: CompilerConfigurationProviderImpl(this, rootDisposable, this@TestConfigurationImpl.environmentConfigurators)
            register(CompilerConfigurationProvider::class, environmentProvider)

            register(AssertionsService::class, assertions)
            register(DefaultsProvider::class, defaultsProvider)

            register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(defaultRegisteredDirectives))

            val metaInfoProcessors = additionalMetaInfoProcessors.map { it.invoke(this) }
            register(GlobalMetadataInfoHandler::class, GlobalMetadataInfoHandler(this, metaInfoProcessors))
        }
    }

    override val steps: List<TestStep<*, *>>
    override val afterAnalysisCheckers: List<AfterAnalysisChecker>

    init {
        val afterAnalysisCheckerConstructors = mutableSetOf<Constructor<AfterAnalysisChecker>>()

        this.steps = steps
            .map { it.createTestStep(testServices) }
            .onEach { step ->
                when (step) {
                    is TestStep.FacadeStep<*, *> -> step.facade.registerDirectivesAndServices()
                    is TestStep.HandlersStep<*> -> {
                        step.handlers.registerDirectivesAndServices()
                        step.handlers.flatMapTo(afterAnalysisCheckerConstructors) { it.additionalAfterAnalysisCheckers }
                    }
                }
            }
        afterAnalysisCheckerConstructors.addAll(afterAnalysisCheckers)
        this.afterAnalysisCheckers = afterAnalysisCheckerConstructors.map { constructor ->
            constructor.invoke(testServices).also { it.registerDirectivesAndServices() }
        }
    }

    // ---------------------------------- utils ----------------------------------

    private fun ServicesAndDirectivesContainer.registerDirectivesAndServices() {
        allDirectives += directiveContainers
        testServices.register(additionalServices, skipAlreadyRegistered = true)
    }

    private fun List<ServicesAndDirectivesContainer>.registerDirectivesAndServices() {
        this.forEach { it.registerDirectivesAndServices() }
    }
}

@TestInfrastructureInternals
val TestServices.testConfiguration: TestConfigurationImpl by TestServices.testServiceAccessor()
