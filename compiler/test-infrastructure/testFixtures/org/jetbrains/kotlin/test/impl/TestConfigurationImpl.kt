/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.builders.NonGroupingPhaseTestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.ComposedDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.ServicesAndDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.ModuleStructureExtractorImpl
import org.jetbrains.kotlin.test.utils.TestDisposable
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

@OptIn(TestInfrastructureInternals::class)
sealed class TestConfigurationImplBase<Step : TestStep<*, *>>(
    testInfo: KotlinTestInfo,

    defaultsProvider: DefaultsProvider,
    assertions: AssertionsService,

    steps: List<TestStepBuilder<*, *, Step>>,

    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>,

    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>,
    moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,
    failureSuppressors: List<Constructor<TestFailureSuppressor>>,

    compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?,
    runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>,

    override val metaInfoHandlerEnabled: Boolean,

    directives: List<DirectivesContainer>,
    override val defaultRegisteredDirectives: RegisteredDirectives,
    additionalServices: List<ServiceRegistrationData>,
) : TestConfiguration<Step>, TestService {
    override val rootDisposable: Disposable = TestDisposable("${this::class.simpleName}.rootDisposable")
    override val testServices: TestServices = TestServices()

    init {
        testServices.register(TestConfigurationImplBase::class, this)
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
                EnvironmentConfiguratorsProvider(this@TestConfigurationImplBase.environmentConfigurators)
            )
            val sourceFilePreprocessors = sourcePreprocessors.map { it.invoke(this@apply) }
            val sourceFileProvider = SourceFileProviderImpl(this, sourceFilePreprocessors)
            register(SourceFileProvider::class, sourceFileProvider)

            val environmentProvider =
                compilerConfigurationProvider?.invoke(this, rootDisposable, this@TestConfigurationImplBase.environmentConfigurators)
                    ?: error("CompilerConfigurationProvider is not initialized")
            register(CompilerConfigurationProvider::class, environmentProvider)

            register(AssertionsService::class, assertions)
            register(DefaultsProvider::class, defaultsProvider)

            register(DefaultRegisteredDirectivesProvider::class, DefaultRegisteredDirectivesProvider(defaultRegisteredDirectives))

            val metaInfoProcessors = additionalMetaInfoProcessors.map { it.invoke(this) }
            register(GlobalMetadataInfoHandler::class, GlobalMetadataInfoHandler(this, metaInfoProcessors))
        }
    }

    final override val steps: List<Step>
    final override val afterAnalysisCheckers: List<AfterAnalysisChecker>
    final override val failureSuppressors: List<TestFailureSuppressor>

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
        }.sortedBy { it.order }

        this.failureSuppressors = failureSuppressors.map { constructor ->
            constructor.invoke(testServices).also { it.registerDirectivesAndServices() }
        }.sortedBy { it.order }
    }

    // ---------------------------------- utils ----------------------------------

    protected fun ServicesAndDirectivesContainer.registerDirectivesAndServices() {
        allDirectives += directiveContainers
        testServices.register(additionalServices, skipAlreadyRegistered = true)
    }

    protected fun List<ServicesAndDirectivesContainer>.registerDirectivesAndServices() {
        this.forEach { it.registerDirectivesAndServices() }
    }
}

@OptIn(TestInfrastructureInternals::class)
class NonGroupingPhaseTestConfigurationImpl(
    testInfo: KotlinTestInfo,
    defaultsProvider: DefaultsProvider,
    assertions: AssertionsService,
    steps: List<TestStepBuilder<*, *, TestStep.NonGroupingStep<*, *>>>,
    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>,
    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>,
    moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,
    failureSuppressors: List<Constructor<TestFailureSuppressor>>,
    compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?,
    runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>,
    groupingTestIsolators: List<Constructor<GroupingTestIsolator>>,
    metaInfoHandlerEnabled: Boolean,
    directives: List<DirectivesContainer>,
    defaultRegisteredDirectives: RegisteredDirectives,
    override var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>,
    additionalServices: List<ServiceRegistrationData>,
    val originalBuilder: NonGroupingPhaseTestConfigurationBuilder.ReadOnlyBuilder,
) : TestConfigurationImplBase<TestStep.NonGroupingStep<*, *>>(
    testInfo, defaultsProvider, assertions, steps, sourcePreprocessors, additionalMetaInfoProcessors, environmentConfigurators,
    additionalSourceProviders, preAnalysisHandlers, moduleStructureTransformers, metaTestConfigurators, afterAnalysisCheckers,
    failureSuppressors, compilerConfigurationProvider, runtimeClasspathProviders, metaInfoHandlerEnabled, directives,
    defaultRegisteredDirectives, additionalServices
), NonGroupingPhaseTestConfiguration {
    override val groupingTestIsolators: List<GroupingTestIsolator> = groupingTestIsolators.map { it.invoke(testServices) }.also {
        it.registerDirectivesAndServices()
    }
}

@OptIn(TestInfrastructureInternals::class)
class GroupingPhaseTestConfigurationImpl(
    testInfo: KotlinTestInfo,
    defaultsProvider: DefaultsProvider,
    assertions: AssertionsService,
    steps: List<TestStepBuilder<*, *, TestStep.GroupingPhaseStep<*, *>>>,
    sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>,
    additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>,
    environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>,
    additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>,
    preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>,
    moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>,
    metaTestConfigurators: List<Constructor<MetaTestConfigurator>>,
    afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>,
    failureSuppressors: List<Constructor<TestFailureSuppressor>>,
    compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?,
    runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>,
    metaInfoHandlerEnabled: Boolean,
    directives: List<DirectivesContainer>,
    defaultRegisteredDirectives: RegisteredDirectives,
    mergerWorkers: List<Constructor<GroupingPhaseInputsMerger.Worker>>,
    additionalServices: List<ServiceRegistrationData>,
) : TestConfigurationImplBase<TestStep.GroupingPhaseStep<*, *>>(
    testInfo, defaultsProvider, assertions, steps, sourcePreprocessors, additionalMetaInfoProcessors, environmentConfigurators,
    additionalSourceProviders, preAnalysisHandlers, moduleStructureTransformers, metaTestConfigurators, afterAnalysisCheckers,
    failureSuppressors, compilerConfigurationProvider, runtimeClasspathProviders, metaInfoHandlerEnabled, directives,
    defaultRegisteredDirectives, additionalServices,
), GroupingPhaseTestConfiguration {
    override val mergerWorkers: List<GroupingPhaseInputsMerger.Worker> = mergerWorkers.map { it.invoke(testServices) }
}


@TestInfrastructureInternals
val TestServices.testConfiguration: TestConfigurationImplBase<*> by TestServices.testServiceAccessor()

@OptIn(TestInfrastructureInternals::class)
fun TestServices.shouldIsolateTestInGroupingConfiguration(testModuleStructure: TestModuleStructure, fileGenerationPhase: Boolean): Boolean {
    val groupingTestIsolators = (testConfiguration as NonGroupingPhaseTestConfiguration).groupingTestIsolators
        .applyIf(fileGenerationPhase) { filter { it.affectsFileGenerators } }
    return groupingTestIsolators.any {
        it.computeBatchToken(testModuleStructure) == GroupingTestIsolator.BatchToken.Isolated
    }
}

@OptIn(TestInfrastructureInternals::class)
fun TestServices.shouldIsolateTestInGroupingConfiguration(fileGenerationPhase: Boolean): Boolean {
    return shouldIsolateTestInGroupingConfiguration(moduleStructure, fileGenerationPhase)
}
