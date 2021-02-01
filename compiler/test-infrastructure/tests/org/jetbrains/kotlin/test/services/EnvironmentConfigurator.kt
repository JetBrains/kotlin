/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.test.directives.model.*
import org.jetbrains.kotlin.test.model.TestModule

abstract class EnvironmentConfigurator(protected val testServices: TestServices) {
    open val directivesContainers: List<DirectivesContainer>
        get() = emptyList()

    open val additionalServices: List<ServiceRegistrationData>
        get() = emptyList()

    protected val moduleStructure: TestModuleStructure
        get() = testServices.moduleStructure

    protected open fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule, project: MockProject) {}

    fun configureCompileConfigurationWithAdditionalConfigurationKeys(
        configuration: CompilerConfiguration,
        module: TestModule,
        project: MockProject
    ) {
        configureCompilerConfiguration(configuration, module, project)
        val extractor = DirectiveToConfigurationKeyExtractor()
        extractor.provideConfigurationKeys()
        extractor.configure(configuration, module.directives)
    }

    protected open fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {}

    open fun provideAdditionalAnalysisFlags(directives: RegisteredDirectives): Map<AnalysisFlag<*>, Any?> {
        return emptyMap()
    }
}

class DirectiveToConfigurationKeyExtractor {
    private val booleanDirectivesMap = mutableMapOf<SimpleDirective, CompilerConfigurationKey<Boolean>>()
    private val invertedBooleanDirectives = mutableSetOf<SimpleDirective>()
    private val valueDirectivesMap = mutableMapOf<ValueDirective<*>, CompilerConfigurationKey<*>>()

    fun register(
        directive: SimpleDirective,
        key: CompilerConfigurationKey<Boolean>,
        isInverted: Boolean = false
    ) {
        booleanDirectivesMap[directive] = key
        if (isInverted) {
            invertedBooleanDirectives += directive
        }
    }

    fun <T : Any> register(
        directive: ValueDirective<T>,
        key: CompilerConfigurationKey<T>
    ) {
        valueDirectivesMap[directive] = key
    }

    fun configure(configuration: CompilerConfiguration, registeredDirectives: RegisteredDirectives) {
        for ((directive, key) in booleanDirectivesMap) {
            if (directive in registeredDirectives) {
                val value = directive !in invertedBooleanDirectives
                configuration.put(key, value)
            }
        }
        for ((directive, key) in valueDirectivesMap) {
            val value = registeredDirectives.singleOrZeroValue(directive) ?: continue
            @Suppress("UNCHECKED_CAST")
            configuration.put(key as CompilerConfigurationKey<Any>, value)
        }
    }
}
