/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.allowKotlinPackage
import org.jetbrains.kotlin.cli.common.allowNoSourceFiles
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.parseAnalysisFlags
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.cliBasedFacadesEnabled
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.test.services.isKtsFile
import org.jetbrains.kotlin.test.services.isLeafModuleInMppGraph
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.test.services.transitiveDependsOnDependencies

class CommonEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ConfigurationDirectives)

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return buildMap {
            put(allowFullyQualifiedNameInKClass, true)
            if (DISABLE_TYPEALIAS_EXPANSION in directives) {
                put(AnalysisFlags.expandTypeAliasesInTypeResolution, false)
            }
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val rawFlags = module.directives[ConfigurationDirectives.KOTLIN_CONFIGURATION_FLAGS]
        parseAnalysisFlags(rawFlags).forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            configuration.put(key as CompilerConfigurationKey<Any>, value)
        }

        if (module.targetPlatform(testServices).isCommon() && WITH_STDLIB in module.directives) {
            configuration.add(
                CLIConfigurationKeys.CONTENT_ROOTS,
                JvmClasspathRoot(ForTestCompileRuntime.stdlibCommonForTests())
            )
        }

        setupK2CliConfiguration(module, configuration)
    }

    private fun setupK2CliConfiguration(
        module: TestModule,
        configuration: CompilerConfiguration,
    ) {
        if (!testServices.cliBasedFacadesEnabled) return
        if (
            module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) &&
            module.isLeafModuleInMppGraph(testServices)
        ) {
            val hmppModules = module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)
                .associateWith {
                    HmppCliModule(
                        name = it.name,
                        sources = it.kotlinFiles
                            .map { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it).canonicalPath }
                            .toSet()
                    )
                }

            val dependencyMap = buildMap {
                for ((testModule, cliModule) in hmppModules) {
                    put(cliModule, testModule.dependsOnDependencies.map { hmppModules.getValue(it.dependencyModule) })
                }
            }
            configuration.hmppModuleStructure = HmppCliModuleStructure(hmppModules.values.toList(), dependencyMap)
        }

        if (FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
            configuration.useFirExtraCheckers = true
        }
        if (FirDiagnosticsDirectives.WITH_EXPERIMENTAL_CHECKERS in module.directives) {
            configuration.useFirExperimentalCheckers = true
        }
        when (module.directives.singleOrZeroValue(FirDiagnosticsDirectives.FIR_PARSER)) {
            FirParser.Psi -> configuration.useLightTree = false
            FirParser.LightTree -> configuration.useLightTree = true
            null -> {}
        }
        configuration.allowNoSourceFiles = true
        configuration.allowAnyScriptsInSourceRoots = true
        configuration.allowKotlinPackage = true
        configuration.dontCreateSeparateSessionForScripts = true
    }
}

val TestModule.kotlinFiles: List<TestFile>
    get() = files.filter { it.isKtFile || it.isKtsFile }
