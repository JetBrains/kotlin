/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.test.TestConfiguration
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.impl.TestConfigurationImpl
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*

typealias Constructor<T> = (TestServices) -> T

@DefaultsDsl
class TestConfigurationBuilder {
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder()
    lateinit var assertions: AssertionsService

    private val facades: MutableList<Constructor<AbstractTestFacade<*, *>>> = mutableListOf()

    private val handlers: MutableList<Constructor<AnalysisHandler<*>>> = mutableListOf()

    private val sourcePreprocessors: MutableList<Constructor<SourceFilePreprocessor>> = mutableListOf()
    private val additionalMetaInfoProcessors: MutableList<Constructor<AdditionalMetaInfoProcessor>> = mutableListOf()
    private val environmentConfigurators: MutableList<Constructor<EnvironmentConfigurator>> = mutableListOf()

    private val additionalSourceProviders: MutableList<Constructor<AdditionalSourceProvider>> = mutableListOf()

    private val metaTestConfigurators: MutableList<Constructor<MetaTestConfigurator>> = mutableListOf()
    private val afterAnalysisCheckers: MutableList<Constructor<AfterAnalysisChecker>> = mutableListOf()

    private var metaInfoHandlerEnabled: Boolean = false

    private val directives: MutableList<DirectivesContainer> = mutableListOf()
    val defaultRegisteredDirectivesBuilder: RegisteredDirectivesBuilder = RegisteredDirectivesBuilder()

    private val configurationsByTestDataCondition: MutableList<Pair<Regex, TestConfigurationBuilder.() -> Unit>> = mutableListOf()
    private val additionalServices: MutableList<ServiceRegistrationData> = mutableListOf()

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

    infix fun String.or(other: String): String {
        return """$this|$other"""
    }

    private fun String.toMatchingRegexString(): String = """^${replace("*", ".*")}$"""

    fun forTestsMatching(pattern: Regex, configuration: TestConfigurationBuilder.() -> Unit) {
        configurationsByTestDataCondition += pattern to configuration
    }

    inline fun globalDefaults(init: DefaultsProviderBuilder.() -> Unit) {
        defaultsProviderBuilder.apply(init)
    }

    fun unregisterAllFacades() {
        facades.clear()
    }

    fun useFrontendFacades(vararg constructor: Constructor<FrontendFacade<*>>) {
        facades += constructor
    }

    fun useBackendFacades(vararg constructor: Constructor<BackendFacade<*, *>>) {
        facades += constructor
    }

    fun useFrontend2BackendConverters(vararg constructor: Constructor<Frontend2BackendConverter<*, *>>) {
        facades += constructor
    }

    fun useFrontendHandlers(vararg constructor: Constructor<FrontendOutputHandler<*>>) {
        handlers += constructor
    }

    fun useBackendHandlers(vararg constructor: Constructor<BackendInputHandler<*>>) {
        handlers += constructor
    }

    fun useArtifactsHandlers(vararg constructor: Constructor<BinaryArtifactHandler<*>>) {
        handlers += constructor
    }

    fun useSourcePreprocessor(vararg preprocessors: Constructor<SourceFilePreprocessor>) {
        sourcePreprocessors += preprocessors
    }

    fun useDirectives(vararg directives: DirectivesContainer) {
        this.directives += directives
    }

    fun useConfigurators(vararg environmentConfigurators: Constructor<EnvironmentConfigurator>) {
        this.environmentConfigurators += environmentConfigurators
    }

    fun useMetaInfoProcessors(vararg updaters: Constructor<AdditionalMetaInfoProcessor>) {
        additionalMetaInfoProcessors += updaters
    }

    fun useAdditionalSourceProviders(vararg providers: Constructor<AdditionalSourceProvider>) {
        additionalSourceProviders += providers
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
        for ((regex, configuration) in configurationsByTestDataCondition) {
            if (regex.matches(testDataPath)) {
                this.configuration()
            }
        }
        return TestConfigurationImpl(
            defaultsProviderBuilder.build(),
            assertions,
            facades,
            handlers,
            sourcePreprocessors,
            additionalMetaInfoProcessors,
            environmentConfigurators,
            additionalSourceProviders,
            metaTestConfigurators,
            afterAnalysisCheckers,
            metaInfoHandlerEnabled,
            directives,
            defaultRegisteredDirectivesBuilder.build(),
            additionalServices
        )
    }
}

inline fun testConfiguration(testDataPath: String, init: TestConfigurationBuilder.() -> Unit): TestConfiguration {
    return TestConfigurationBuilder().apply(init).build(testDataPath)
}
