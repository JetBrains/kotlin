/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.ServicesAndDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule

abstract class EnvironmentConfigurator(protected val testServices: TestServices) : ServicesAndDirectivesContainer {
    protected val moduleStructure: TestModuleStructure
        get() = testServices.moduleStructure

    protected open fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {}

    fun configureCompileConfigurationWithAdditionalConfigurationKeys(
        configuration: CompilerConfiguration,
        module: TestModule,
    ) {
        val extractor = DirectiveToConfigurationKeyExtractor()
        extractor.provideConfigurationKeys()
        extractor.configure(configuration, module.directives)
        configureCompilerConfiguration(configuration, module)
    }

    open fun DirectiveToConfigurationKeyExtractor.provideConfigurationKeys() {}

    open fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return emptyMap()
    }

    open fun registerCompilerExtensions(project: Project) {}
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
