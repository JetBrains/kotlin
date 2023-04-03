/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.parseAnalysisFlags
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

class CommonEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ConfigurationDirectives)

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return super.provideAdditionalAnalysisFlags(directives, languageVersion).toMutableMap().also {
            it[allowFullyQualifiedNameInKClass] = true
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val rawFlags = module.directives[ConfigurationDirectives.KOTLIN_CONFIGURATION_FLAGS]
        parseAnalysisFlags(rawFlags).forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            configuration.put(key as CompilerConfigurationKey<Any>, value)
        }

        if (module.targetPlatform in CommonPlatforms.allDefaultTargetPlatforms) {
            if (WITH_STDLIB in module.directives) {
                configuration.add(
                    CLIConfigurationKeys.CONTENT_ROOTS,
                    JvmClasspathRoot(ForTestCompileRuntime.stdlibCommonForTests())
                )
            }
        }
    }
}
