/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.impl.TestConfigurationImpl
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import kotlin.io.path.Path

@DefaultsDsl
@OptIn(TestInfrastructureInternals::class, PrivateForInline::class)
class TestConfigurationBuilder {
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder()
    lateinit var assertions: AssertionsService

    @PrivateForInline
    val steps: MutableList<TestStepBuilder<*, *>> = mutableListOf()

    @PrivateForInline
    val namedSteps: MutableMap<String, TestStepBuilder<*, *>> = mutableMapOf()

    private val sourcePreprocessors: MutableList<Constructor<SourceFilePreprocessor>> = mutableListOf()
    private val additionalMetaInfoProcessors: MutableList<Constructor<AdditionalMetaInfoProcessor>> = mutableListOf()
    private val environmentConfigurators: MutableList<Constructor<AbstractEnvironmentConfigurator>> = mutableListOf()
    private val preAnalysisHandlers: MutableList<Constructor<PreAnalysisHandler>> = mutableListOf()

    private val additionalSourceProviders: MutableList<Constructor<AdditionalSourceProvider>> = mutableListOf()
    private val moduleStructureTransformers: MutableList<Constructor<ModuleStructureTransformer>> = mutableListOf()

    private val metaTestConfigurators: MutableList<Constructor<MetaTestConfigurator>> = mutableListOf()
    private val afterAnalysisCheckers: MutableList<Constructor<AfterAnalysisChecker>> = mutableListOf()

    private var metaInfoHandlerEnabled: Boolean = false

    private val directives: MutableList<DirectivesContainer> = mutableListOf()
    val defaultRegisteredDirectivesBuilder: RegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    private val configurationsByPositiveTestDataCondition: MutableList<Pair<Regex, TestConfigurationBuilder.() -> Unit>> = mutableListOf()
    private val configurationsByNegativeTestDataCondition: MutableList<Pair<Regex, TestConfigurationBuilder.() -> Unit>> = mutableListOf()
    private val additionalServices: MutableList<ServiceRegistrationData> = mutableListOf()

    private var compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)? = null
    private var runtimeClasspathProviders: MutableList<Constructor<RuntimeClasspathProvider>> = mutableListOf()

    lateinit var testInfo: KotlinTestInfo

    lateinit var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>

    private val globalDefaultsConfigurators: MutableList<DefaultsProviderBuilder.() -> Unit> = mutableListOf()
    private val defaultDirectiveConfigurators: MutableList<RegisteredDirectivesBuilder.() -> Unit> = mutableListOf()

    inline fun <reified T : TestService> useAdditionalService(noinline serviceConstructor: (TestServices) -> T) {
        useAdditionalServices(service(serviceConstructor))
    }

    fun useAdditionalServices(vararg serviceRegistrationData: ServiceRegistrationData) {
        additionalServices += serviceRegistrationData
    }

    fun forTestsMatching(pattern: String, configuration: TestConfigurationBuilder.() -> Unit) {
        val regex = pattern.toMatchingRegexString().toRegex()
        forTestsMatching(regex, configuration)
    }

    fun forTestsNotMatching(pattern: String, configuration: TestConfigurationBuilder.() -> Unit) {
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

    fun forTestsMatching(pattern: Regex, configuration: TestConfigurationBuilder.() -> Unit) {
        configurationsByPositiveTestDataCondition += pattern to configuration
    }

    fun forTestsNotMatching(pattern: Regex, configuration: TestConfigurationBuilder.() -> Unit) {
        configurationsByNegativeTestDataCondition += pattern to configuration
    }

    fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        globalDefaultsConfigurators += init
        defaultsProviderBuilder.apply(init)
    }

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractTestFacade<I, O>>,
    ): FacadeStepBuilder<I, O> {
        return FacadeStepBuilder(facade).also {
            steps += it
        }
    }

    inline fun <InputArtifact, InputArtifactKind> handlersStep(
        artifactKind: InputArtifactKind,
        init: HandlersStepBuilder<InputArtifact, InputArtifactKind>.() -> Unit,
    ): HandlersStepBuilder<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        return HandlersStepBuilder(artifactKind).also {
            it.init()
            steps += it
        }
    }

    inline fun <InputArtifact, InputArtifactKind> namedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        init: HandlersStepBuilder<InputArtifact, InputArtifactKind>.() -> Unit,
    ): HandlersStepBuilder<InputArtifact, InputArtifactKind>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact> {
        val previouslyContainedStep = namedStepOfType<InputArtifact, InputArtifactKind>(name)
        return if (previouslyContainedStep == null) {
            val step = handlersStep(artifactKind, init)
            namedSteps[name] = step
            step
        } else {
            configureNamedHandlersStep(name, artifactKind, init)
            previouslyContainedStep
        }
    }

    inline fun <InputArtifact, InputArtifactKind> configureNamedHandlersStep(
        name: String,
        artifactKind: InputArtifactKind,
        init: HandlersStepBuilder<InputArtifact, InputArtifactKind>.() -> Unit
    ) where InputArtifact : ResultingArtifact<InputArtifact>,
            InputArtifactKind : TestArtifactKind<InputArtifact> {
        val step = namedStepOfType<InputArtifact, InputArtifactKind>(name) ?: error { "Step \"$name\" not found" }
        require(step.artifactKind == artifactKind) { "Step kind: ${step.artifactKind}, passed kind is $artifactKind" }
        step.apply(init)
    }

    fun <InputArtifact, InputArtifactKind> namedStepOfType(name: String): HandlersStepBuilder<InputArtifact, InputArtifactKind>?
        where InputArtifact : ResultingArtifact<InputArtifact>,
              InputArtifactKind : TestArtifactKind<InputArtifact> {
        @Suppress("UNCHECKED_CAST")
        return namedSteps[name] as HandlersStepBuilder<InputArtifact, InputArtifactKind>?
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
    fun resetModuleStructureTransformers() {
        moduleStructureTransformers.clear()
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

    fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
        defaultDirectiveConfigurators += init
        defaultRegisteredDirectivesBuilder.apply(init)
    }

    fun enableMetaInfoHandler() {
        metaInfoHandlerEnabled = true
    }

    fun build(testDataPath: String): TestConfiguration {
        // We use URI here because we use '/' in our codebase, and URI also uses it (unlike OS-dependent `toString()`)
        val absoluteTestDataPath = Path(testDataPath).normalize().toUri().toString()

        for ((regex, configuration) in configurationsByPositiveTestDataCondition) {
            if (regex.matches(absoluteTestDataPath)) {
                this.configuration()
            }
        }
        for ((regex, configuration) in configurationsByNegativeTestDataCondition) {
            if (!regex.matches(absoluteTestDataPath)) {
                this.configuration()
            }
        }
        return TestConfigurationImpl(
            testInfo,
            defaultsProviderBuilder.build(),
            assertions,
            steps,
            sourcePreprocessors,
            additionalMetaInfoProcessors,
            environmentConfigurators,
            additionalSourceProviders,
            preAnalysisHandlers,
            moduleStructureTransformers,
            metaTestConfigurators,
            afterAnalysisCheckers,
            compilerConfigurationProvider,
            runtimeClasspathProviders,
            metaInfoHandlerEnabled,
            directives,
            defaultRegisteredDirectivesBuilder.build(),
            startingArtifactFactory,
            additionalServices,
            originalBuilder = ReadOnlyBuilder(this, testDataPath)
        )
    }

    class ReadOnlyBuilder(private val builder: TestConfigurationBuilder, val testDataPath: String) {
        val defaultsProviderBuilder: DefaultsProviderBuilder
            get() = builder.defaultsProviderBuilder
        val assertions: AssertionsService
            get() = builder.assertions
        val sourcePreprocessors: List<Constructor<SourceFilePreprocessor>>
            get() = builder.sourcePreprocessors
        val additionalMetaInfoProcessors: List<Constructor<AdditionalMetaInfoProcessor>>
            get() = builder.additionalMetaInfoProcessors
        val environmentConfigurators: List<Constructor<AbstractEnvironmentConfigurator>>
            get() = builder.environmentConfigurators
        val preAnalysisHandlers: List<Constructor<PreAnalysisHandler>>
            get() = builder.preAnalysisHandlers
        val additionalSourceProviders: List<Constructor<AdditionalSourceProvider>>
            get() = builder.additionalSourceProviders
        val moduleStructureTransformers: List<Constructor<ModuleStructureTransformer>>
            get() = builder.moduleStructureTransformers
        val metaTestConfigurators: List<Constructor<MetaTestConfigurator>>
            get() = builder.metaTestConfigurators
        val afterAnalysisCheckers: List<Constructor<AfterAnalysisChecker>>
            get() = builder.afterAnalysisCheckers
        val metaInfoHandlerEnabled: Boolean
            get() = builder.metaInfoHandlerEnabled
        val directives: List<DirectivesContainer>
            get() = builder.directives

        val defaultDirectiveConfigurators: List<RegisteredDirectivesBuilder.() -> Unit>
            get() = builder.defaultDirectiveConfigurators

        val globalDefaultsConfigurators: List<DefaultsProviderBuilder.() -> Unit>
            get() = builder.globalDefaultsConfigurators

        val configurationsByPositiveTestDataCondition: List<Pair<Regex, TestConfigurationBuilder.() -> Unit>>
            get() = builder.configurationsByPositiveTestDataCondition
        val configurationsByNegativeTestDataCondition: List<Pair<Regex, TestConfigurationBuilder.() -> Unit>>
            get() = builder.configurationsByNegativeTestDataCondition
        val additionalServices: List<ServiceRegistrationData>
            get() = builder.additionalServices

        val compilerConfigurationProvider: ((TestServices, Disposable, List<AbstractEnvironmentConfigurator>) -> CompilerConfigurationProvider)?
            get() = builder.compilerConfigurationProvider
        val runtimeClasspathProviders: List<Constructor<RuntimeClasspathProvider>>
            get() = builder.runtimeClasspathProviders
        val testInfo: KotlinTestInfo
            get() = builder.testInfo
        val startingArtifactFactory: (TestModule) -> ResultingArtifact<*>
            get() = builder.startingArtifactFactory
    }
}

inline fun testConfiguration(testDataPath: String, init: TestConfigurationBuilder.() -> Unit): TestConfiguration {
    return TestConfigurationBuilder().apply(init).build(testDataPath)
}
