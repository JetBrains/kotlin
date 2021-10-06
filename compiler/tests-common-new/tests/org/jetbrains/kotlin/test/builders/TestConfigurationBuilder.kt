/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.fir.PrivateForInline
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
    private val environmentConfigurators: MutableList<Constructor<EnvironmentConfigurator>> = mutableListOf()
    private val preAnalysisHandlers: MutableList<Constructor<PreAnalysisHandler>> = mutableListOf()

    private val additionalSourceProviders: MutableList<Constructor<AdditionalSourceProvider>> = mutableListOf()
    private val moduleStructureTransformers: MutableList<ModuleStructureTransformer> = mutableListOf()

    private val metaTestConfigurators: MutableList<Constructor<MetaTestConfigurator>> = mutableListOf()
    private val afterAnalysisCheckers: MutableList<Constructor<AfterAnalysisChecker>> = mutableListOf()

    private var metaInfoHandlerEnabled: Boolean = false

    private val directives: MutableList<DirectivesContainer> = mutableListOf()
    val defaultRegisteredDirectivesBuilder: RegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    private val configurationsByPositiveTestDataCondition: MutableList<Pair<Regex, TestConfigurationBuilder.() -> Unit>> = mutableListOf()
    private val configurationsByNegativeTestDataCondition: MutableList<Pair<Regex, TestConfigurationBuilder.() -> Unit>> = mutableListOf()
    private val additionalServices: MutableList<ServiceRegistrationData> = mutableListOf()

    private var compilerConfigurationProvider: ((Disposable, List<EnvironmentConfigurator>) -> CompilerConfigurationProvider)? = null
    private var runtimeClasspathProviders: MutableList<Constructor<RuntimeClasspathProvider>> = mutableListOf()

    lateinit var testInfo: KotlinTestInfo

    lateinit var startingArtifactFactory: (TestModule) -> ResultingArtifact<*>

    inline fun <reified T : TestService> useAdditionalService(noinline serviceConstructor: (TestServices) -> T) {
        useAdditionalService(service(serviceConstructor))
    }

    fun useAdditionalService(serviceRegistrationData: ServiceRegistrationData) {
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

    inline fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        defaultsProviderBuilder.apply(init)
    }

    fun <I : ResultingArtifact<I>, O : ResultingArtifact<O>> facadeStep(
        facade: Constructor<AbstractTestFacade<I, O>>,
    ): FacadeStepBuilder<I, O> {
        return FacadeStepBuilder(facade).also {
            steps += it
        }
    }

    inline fun <I : ResultingArtifact<I>> handlersStep(
        artifactKind: TestArtifactKind<I>,
        init: HandlersStepBuilder<I>.() -> Unit
    ): HandlersStepBuilder<I> {
        return HandlersStepBuilder(artifactKind).also {
            it.init()
            steps += it
        }
    }

    inline fun <I : ResultingArtifact<I>> namedHandlersStep(
        name: String,
        artifactKind: TestArtifactKind<I>,
        init: HandlersStepBuilder<I>.() -> Unit
    ): HandlersStepBuilder<I> {
        val previouslyContainedStep = namedStepOfType<I>(name)
        if (previouslyContainedStep == null) {
            val step = handlersStep(artifactKind, init)
            namedSteps[name] = step
            return step
        } else {
            configureNamedHandlersStep(name, artifactKind, init)
            return previouslyContainedStep
        }
    }

    inline fun <I : ResultingArtifact<I>> configureNamedHandlersStep(
        name: String,
        artifactKind: TestArtifactKind<I>,
        init: HandlersStepBuilder<I>.() -> Unit
    ) {
        val step = namedStepOfType<I>(name) ?: error { "Step \"$name\" not found" }
        require(step.artifactKind == artifactKind) { "Step kind: ${step.artifactKind}, passed kind is $artifactKind" }
        step.apply(init)
    }

    fun <I : ResultingArtifact<I>> namedStepOfType(name: String):  HandlersStepBuilder<I>?  {
        @Suppress("UNCHECKED_CAST")
        return namedSteps[name] as HandlersStepBuilder<I>?
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

    fun useConfigurators(vararg environmentConfigurators: Constructor<EnvironmentConfigurator>) {
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
        moduleStructureTransformers += transformers
    }

    @TestInfrastructureInternals
    fun useCustomCompilerConfigurationProvider(provider: (Disposable, List<EnvironmentConfigurator>) -> CompilerConfigurationProvider) {
        compilerConfigurationProvider = provider
    }

    fun useCustomRuntimeClasspathProviders(vararg provider: Constructor<RuntimeClasspathProvider>) {
        runtimeClasspathProviders += provider
    }

    fun useMetaTestConfigurators(vararg configurators: Constructor<MetaTestConfigurator>) {
        metaTestConfigurators += configurators
    }

    fun useAfterAnalysisCheckers(vararg checkers: Constructor<AfterAnalysisChecker>) {
        afterAnalysisCheckers += checkers
    }

    inline fun defaultDirectives(init: RegisteredDirectivesBuilder.() -> Unit) {
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
            additionalServices
        )
    }
}

inline fun testConfiguration(testDataPath: String, init: TestConfigurationBuilder.() -> Unit): TestConfiguration {
    return TestConfigurationBuilder().apply(init).build(testDataPath)
}
