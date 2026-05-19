/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.handlers.UpdateTestDataHandler
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.impl.GroupingStageTestConfigurationImpl
import org.jetbrains.kotlin.test.impl.NonGroupingStageTestConfigurationImpl
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.PrivateForInline
import kotlin.io.path.Path

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
abstract class TestConfigurationBuilderBase<Self : TestConfigurationBuilderBase<Self, C>, C : TestConfiguration<*>> {
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder()
    lateinit var assertions: AssertionsService

    protected val sourcePreprocessors: MutableList<Constructor<SourceFilePreprocessor>> = mutableListOf()
    protected val additionalMetaInfoProcessors: MutableList<Constructor<AdditionalMetaInfoProcessor>> = mutableListOf()
    protected val environmentConfigurators: MutableList<Constructor<AbstractEnvironmentConfigurator>> = mutableListOf()
    protected val preAnalysisHandlers: MutableList<Constructor<PreAnalysisHandler>> = mutableListOf()

    protected val additionalSourceProviders: MutableList<Constructor<AdditionalSourceProvider>> = mutableListOf()
    protected val moduleStructureTransformers: MutableList<Constructor<ModuleStructureTransformer>> = mutableListOf()

    protected val metaTestConfigurators: MutableList<Constructor<MetaTestConfigurator>> = mutableListOf()
    protected val afterAnalysisCheckers: MutableList<Constructor<AfterAnalysisChecker>> = mutableListOf()
    protected val failureSuppressors: MutableList<Constructor<TestFailureSuppressor>> = mutableListOf()

    protected var metaInfoHandlerEnabled: Boolean = false

    protected val directives: MutableList<DirectivesContainer> = mutableListOf()
    val defaultRegisteredDirectivesBuilder: RegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    protected val configurationsByPositiveTestDataCondition: MutableList<Pair<Regex, Self.() -> Unit>> = mutableListOf()
    protected val configurationsByNegativeTestDataCondition: MutableList<Pair<Regex, Self.() -> Unit>> = mutableListOf()
    protected val additionalServices: MutableList<ServiceRegistrationData> = mutableListOf()

    protected var compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)? =
        null
    protected var runtimeClasspathProviders: MutableList<Constructor<RuntimeClasspathProvider>> = mutableListOf()

    protected val globalDefaultsConfigurators: MutableList<DefaultsProviderBuilder.() -> Unit> = mutableListOf()
    protected val defaultDirectiveConfigurators: MutableList<RegisteredDirectivesBuilder.() -> Unit> = mutableListOf()

    // ------------------------------------------------------------------------------------------------------------

    inline fun <reified T : TestService> useAdditionalService(noinline serviceConstructor: (TestServices) -> T) {
        useAdditionalServices(service(serviceConstructor))
    }

    fun useAdditionalServices(vararg serviceRegistrationData: ServiceRegistrationData) {
        additionalServices += serviceRegistrationData
    }

    fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        globalDefaultsConfigurators += init
        defaultsProviderBuilder.apply(init)
    }

    fun useSourcePreprocessor(vararg preprocessors: Constructor<SourceFilePreprocessor>, needToPrepend: Boolean = false) {
        if (needToPrepend) {
            sourcePreprocessors.addAll(0, preprocessors.toList())
        } else {
            sourcePreprocessors.addAll(preprocessors)
        }
    }

    fun useDirectives(vararg directives: DirectivesContainer) {
        this.directives += directives
    }

    fun useConfigurators(vararg environmentConfigurators: Constructor<AbstractEnvironmentConfigurator>) {
        this.environmentConfigurators += environmentConfigurators
    }

    fun usePreAnalysisHandlers(vararg handlers: Constructor<PreAnalysisHandler>) {
        this.preAnalysisHandlers += handlers
    }

    fun useMetaInfoProcessors(vararg updaters: Constructor<AdditionalMetaInfoProcessor>) {
        additionalMetaInfoProcessors += updaters
    }

    fun useAdditionalSourceProviders(vararg providers: Constructor<AdditionalSourceProvider>) {
        additionalSourceProviders += providers
    }

    @TestInfrastructureInternals
    fun useModuleStructureTransformers(vararg transformers: ModuleStructureTransformer) {
        for (transformer in transformers) {
            moduleStructureTransformers += { _ -> transformer }
        }
    }

    @TestInfrastructureInternals
    fun useModuleStructureTransformers(vararg transformers: Constructor<ModuleStructureTransformer>) {
        moduleStructureTransformers += transformers
    }

    @TestInfrastructureInternals
    fun useCustomCompilerConfigurationProvider(provider: (TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider) {
        compilerConfigurationProvider = provider
    }

    fun useCustomRuntimeClasspathProviders(vararg provider: Constructor<RuntimeClasspathProvider>) {
        runtimeClasspathProviders += provider
    }

    fun useMetaTestConfigurators(vararg configurators: Constructor<MetaTestConfigurator>) {
        metaTestConfigurators += configurators
    }

    fun useAfterAnalysisCheckers(vararg checkers: Constructor<AfterAnalysisChecker>, insertAtFirst: Boolean = false) {
        when (insertAtFirst) {
            false -> afterAnalysisCheckers += checkers
            true -> afterAnalysisCheckers.addAll(0, checkers.asList())
        }
    }

    fun useFailureSuppressors(vararg suppressors: Constructor<TestFailureSuppressor>, insertAtFirst: Boolean = false) {
        when (insertAtFirst) {
            false -> failureSuppressors += suppressors
            true -> failureSuppressors.addAll(0, suppressors.asList())
        }
    }

    fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultDirectiveConfigurators += init
        defaultRegisteredDirectivesBuilder.apply(init)
    }

    fun forTestsMatching(pattern: String, configuration: Self.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        forTestsMatching(regex, configuration)
    }

    fun forTestsNotMatching(pattern: String, configuration: Self.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        forTestsNotMatching(regex, configuration)
    }

    infix fun String.or(other: String): String {
        return """$this|$other"""
    }

    private fun String.toMatchingRegexString(): String = when (this) {
        "*" -> ".*"
        else -> """^.*/(${replace("*", ".*")})$"""
    }

    fun forTestsMatching(pattern: Regex, configuration: Self.() -> Unit) {
        configurationsByPositiveTestDataCondition += pattern to configuration
    }

    fun forTestsNotMatching(pattern: Regex, configuration: Self.() -> Unit) {
        configurationsByNegativeTestDataCondition += pattern to configuration
    }

    abstract fun build(testDataPath: String): C

    protected fun applyConditionalConfigurations(testDataPath: String) {
        // We use URI here because we use '/' in our codebase, and URI also uses it (unlike OS-dependent `toString()`)
        val absoluteTestDataPath = Path(testDataPath).normalize().toUri().toString()

        for ([regex, configuration] in configurationsByPositiveTestDataCondition) {
            if (regex.matches(absoluteTestDataPath)) {
                @Suppress("UNCHECKED_CAST")
                configuration(this as Self)
            }
        }
        for ([regex, configuration] in configurationsByNegativeTestDataCondition) {
            if (!regex.matches(absoluteTestDataPath)) {
                @Suppress("UNCHECKED_CAST")
                configuration(this as Self)
            }
        }
    }

}

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
sealed class OneStageTestConfigurationBuilderBase<Self, C> : TestConfigurationBuilderBase<Self, C>()
        where Self : TestConfigurationBuilderBase<Self, C>,
              C : TestConfiguration<*>
{
    private typealias Step = TestStep<*, *>
    private typealias StepBuilder = TestStepBuilder<*, *, Step>

    @PrivateForInline
    val steps: MutableList<StepBuilder> = mutableListOf()

    @PrivateForInline
    val namedSteps: MutableMap<String, StepBuilder> = mutableMapOf()
}

@OptIn(PrivateForInline::class)
class NonGroupingStageTestConfigurationBuilder :
    OneStageTestConfigurationBuilderBase<NonGroupingStageTestConfigurationBuilder, NonGroupingStageTestConfiguration>() {
    lateinit var testInfo: KotlinTestInfo
    lateinit var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
    private val groupingTestIsolators: MutableList<Constructor<GroupingTestIsolator>> = mutableListOf()

    fun useGroupingTestIsolators(vararg isolators: Constructor<GroupingTestIsolator>) {
        groupingTestIsolators += isolators
    }

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.NonGroupingStage<I, O> {
        return TestStepBuilder.FacadeStepBuilder.NonGroupingStage(facade).also {
            steps.add(it)
        }
    }

    inline fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return TestStepBuilder.HandlersStepBuilder.NonGroupingStage(artifactKind, compilationStage).also {
            it.init()
            steps += it
        }
    }

    inline fun <InputArtifact, InputArtifactKind> namedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        val previouslyContainedStep = namedStepOfType<InputArtifact, InputArtifactKind>(name)
        return if (previouslyContainedStep == null) {
            val step = handlersStep(artifactKind, compilationStage, init)
            namedSteps[name] = step
            step
        } else {
            configureNamedHandlersStep(name, artifactKind, skipMissingStep = false, init)
            previouslyContainedStep
        }
    }

    inline fun <InputArtifact, InputArtifactKind> configureNamedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        skipMissingStep: Boolean = false,
        init: TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>.() -> Unit,
    ) where InputArtifact : ResultingArtifact<InputArtifact>,
            InputArtifactKind : TestArtifactKind<InputArtifact> {
        val step = namedStepOfType<InputArtifact, InputArtifactKind>(name)
            ?: when (skipMissingStep) {
                true -> return
                false -> error("Step \"$name\" not found")
            }
        require(step.artifactKind == artifactKind) { "Step kind: ${step.artifactKind}, passed kind is $artifactKind" }
        step.apply(init)
    }

    fun <InputArtifact, InputArtifactKind> namedStepOfType(name: String): TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>?
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        return namedSteps[name] as TestStepBuilder.HandlersStepBuilder.NonGroupingStage<InputArtifact, InputArtifactKind>?
    }

    fun enableMetaInfoHandler() {
        metaInfoHandlerEnabled = true
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun build(testDataPath: String): NonGroupingStageTestConfiguration {
        applyConditionalConfigurations(testDataPath)

        // UpdateTestDataHandler should be _the very last_ handler at all times to avoid false-positive test data changes,
        // so it is added after all configuration callbacks have already been executed
        useFailureSuppressors(::UpdateTestDataHandler)

        @Suppress("UNCHECKED_CAST")
        return NonGroupingStageTestConfigurationImpl(
            testInfo,
            defaultsProviderBuilder.build(),
            assertions,
            steps as List<TestStepBuilder<*, *, TestStep.NonGroupingStep<*, *>>>,
            sourcePreprocessors,
            additionalMetaInfoProcessors,
            environmentConfigurators,
            additionalSourceProviders,
            preAnalysisHandlers,
            moduleStructureTransformers,
            metaTestConfigurators,
            afterAnalysisCheckers,
            failureSuppressors,
            compilerConfigurationProvider,
            runtimeClasspathProviders,
            groupingTestIsolators,
            metaInfoHandlerEnabled,
            directives,
            defaultRegisteredDirectivesBuilder.build(),
            startingArtifactFactory,
            additionalServices,
            originalBuilder = ReadOnlyBuilder(this, testDataPath)
        )
    }

    class ReadOnlyBuilder(private val builder: NonGroupingStageTestConfigurationBuilder, val testDataPath: String) {
        val assertions: AssertionsService
            get() = builder.assertions
        val sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>
            get() = builder.sourcePreprocessors
        val additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>
            get() = builder.additionalMetaInfoProcessors
        val environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>
            get() = builder.environmentConfigurators
        val directives: List<DirectivesContainer>
            get() = builder.directives

        val defaultDirectiveConfigurators: List<RegisteredDirectivesBuilder.() -> Unit>
            get() = builder.defaultDirectiveConfigurators

        val globalDefaultsConfigurators: List<DefaultsProviderBuilder.() -> Unit>
            get() = builder.globalDefaultsConfigurators

        val additionalServices: List<ServiceRegistrationData>
            get() = builder.additionalServices

        val compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?
            get() = builder.compilerConfigurationProvider
        val testInfo: KotlinTestInfo
            get() = builder.testInfo
        val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
            get() = builder.startingArtifactFactory
    }
}

typealias TestConfigurationBuilder = NonGroupingStageTestConfigurationBuilder

@OptIn(PrivateForInline::class)
class GroupingStageTestConfigurationBuilder :
    OneStageTestConfigurationBuilderBase<GroupingStageTestConfigurationBuilder, GroupingStageTestConfiguration>() {
    lateinit var testInfo: KotlinTestInfo
    val mergerWorkers: MutableList<Constructor<GroupingStageInputsMerger.Worker>> = mutableListOf()

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractGroupingStageTestFacade<I, O>>,
    ): TestStepBuilder.FacadeStepBuilder.GroupingStage<I, O> {
        return TestStepBuilder.FacadeStepBuilder.GroupingStage(facade).also {
            steps.add(it)
        }
    }

    inline fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return TestStepBuilder.HandlersStepBuilder.GroupingStage(artifactKind, compilationStage).also {
            it.init()
            steps += it
        }
    }

    inline fun <InputArtifact, InputArtifactKind> namedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        compilationStage: CompilationStage,
        init: TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>.() -> Unit,
    ): TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        val previouslyContainedStep = namedStepOfType<InputArtifact, InputArtifactKind>(name)
        return if (previouslyContainedStep == null) {
            val step = handlersStep(artifactKind, compilationStage, init)
            namedSteps[name] = step
            step
        } else {
            configureNamedHandlersStep(name, artifactKind, skipMissingStep = false, init)
            previouslyContainedStep
        }
    }

    inline fun <InputArtifact, InputArtifactKind> configureNamedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        skipMissingStep: Boolean = false,
        init: TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>.() -> Unit,
    ) where InputArtifact : ResultingArtifact<InputArtifact>,
            InputArtifactKind : TestArtifactKind<InputArtifact> {
        val step = namedStepOfType<InputArtifact, InputArtifactKind>(name)
            ?: when (skipMissingStep) {
                true -> return
                false -> error("Step \"$name\" not found")
            }
        require(step.artifactKind == artifactKind) { "Step kind: ${step.artifactKind}, passed kind is $artifactKind" }
        step.apply(init)
    }

    fun <InputArtifact, InputArtifactKind> namedStepOfType(name: String): TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>?
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        return namedSteps[name] as TestStepBuilder.HandlersStepBuilder.GroupingStage<InputArtifact, InputArtifactKind>?
    }

    fun withMergerWorker(worker: Constructor<GroupingStageInputsMerger.Worker>) {
        mergerWorkers += worker
    }

    @OptIn(TestInfrastructureInternals::class)
    override fun build(testDataPath: String): GroupingStageTestConfiguration {
        applyConditionalConfigurations(testDataPath)

        // UpdateTestDataHandler should be _the very last_ handler at all times to avoid false-positive test data changes,
        // so it is added after all configuration callbacks have already been executed
        useFailureSuppressors(::UpdateTestDataHandler)

        @Suppress("UNCHECKED_CAST")
        return GroupingStageTestConfigurationImpl(
            testInfo,
            defaultsProviderBuilder.build(),
            assertions,
            steps as List<TestStepBuilder<*, *, TestStep.GroupingStageStep<*, *>>>,
            sourcePreprocessors,
            additionalMetaInfoProcessors,
            environmentConfigurators,
            additionalSourceProviders,
            preAnalysisHandlers,
            moduleStructureTransformers,
            metaTestConfigurators,
            afterAnalysisCheckers,
            failureSuppressors,
            compilerConfigurationProvider,
            runtimeClasspathProviders,
            metaInfoHandlerEnabled,
            directives,
            defaultRegisteredDirectivesBuilder.build(),
            mergerWorkers,
            additionalServices,
        )
    }
}

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
class TwoStageTestConfigurationBuilder {
    val nonGroupingStageBuilder = NonGroupingStageTestConfigurationBuilder()
    val groupingStageBuilder = GroupingStageTestConfigurationBuilder()

    fun commonConfiguration(init: TestConfigurationBuilderBase<*, *>.() -> Unit) {
        nonGroupingStageBuilder.apply(init)
        groupingStageBuilder.apply(init)
    }

    fun nonGroupingStage(init: NonGroupingStageTestConfigurationBuilder.() -> Unit) {
        nonGroupingStageBuilder.apply(init)
    }

    fun groupingStage(init: GroupingStageTestConfigurationBuilder.() -> Unit) {
        groupingStageBuilder.apply(init)
    }
}

inline fun testConfiguration(
    testDataPath: String,
    init: NonGroupingStageTestConfigurationBuilder.() -> Unit,
): NonGroupingStageTestConfiguration {
    return NonGroupingStageTestConfigurationBuilder().apply(init).build(testDataPath)
}
