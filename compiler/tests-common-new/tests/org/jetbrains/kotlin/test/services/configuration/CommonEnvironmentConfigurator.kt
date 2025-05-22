/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.allowKotlinPackage
import org.jetbrains.kotlin.cli.common.allowNoSourceFiles
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.DependencyRelation.DependsOnDependency
import org.jetbrains.kotlin.test.model.DependencyRelation.FriendDependency
import org.jetbrains.kotlin.test.model.DependencyRelation.RegularDependency
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager

class CommonEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(ConfigurationDirectives)

    override fun provideAdditionalAnalysisFlags(
        directives: RegisteredDirectives,
        languageVersion: LanguageVersion
    ): Map<AnalysisFlag<*>, Any?> {
        return buildMap {
            put(allowFullyQualifiedNameInKClass, true)
            put(AnalysisFlags.hierarchicalMultiplatformCompilation, SEPARATE_KMP_COMPILATION in directives)
            if (DISABLE_TYPEALIAS_EXPANSION in directives) {
                put(AnalysisFlags.expandTypeAliasesInTypeResolution, false)
            }
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        configuration.testEnvironment = true
        if (module.targetPlatform(testServices).isCommon() && WITH_STDLIB in module.directives) {
            configuration.add(
                CLIConfigurationKeys.CONTENT_ROOTS,
                JvmClasspathRoot(ForTestCompileRuntime.stdlibCommonForTests())
            )
        }
        if (FirDiagnosticsDirectives.WITH_EXTRA_CHECKERS in module.directives) {
            configuration.useFirExtraCheckers = true
        }
        if (FirDiagnosticsDirectives.DUMP_CONSTRAINTS in module.directives) {
            configuration.dumpConstraints = true
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

            val sourceDependencies = buildMap {
                for ((testModule, cliModule) in hmppModules) {
                    put(cliModule, testModule.dependsOnDependencies.map { hmppModules.getValue(it.dependencyModule) })
                }
            }

            val moduleDependencies = when {
                SEPARATE_KMP_COMPILATION in module.directives -> {
                    buildMap {
                        for ((testModule, cliModule) in hmppModules) {
                            val dependencies = buildList {
                                testModule.allDependencies.mapNotNullTo(this) { dependencyDescription ->
                                    val dependencyOutputDir = testServices.compiledClassesManager.getOutputDirForModule(dependencyDescription.dependencyModule)
                                    configuration.addJvmClasspathRoot(dependencyOutputDir)
                                    when (dependencyDescription.relation) {
                                        RegularDependency, FriendDependency -> dependencyOutputDir.canonicalPath
                                        DependsOnDependency -> null
                                    }
                                }
                                val standardLibrariesPathProvider = testServices.standardLibrariesPathProvider
                                add(standardLibrariesPathProvider.commonStdlibForTests().canonicalPath)
                            }
                            put(cliModule, dependencies)
                        }
                    }
                }
                else -> emptyMap()
            }

            configuration.hmppModuleStructure = HmppCliModuleStructure(
                hmppModules.values.toList(),
                sourceDependencies,
                moduleDependencies
            )
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
        configuration.dontSortSourceFiles = true
    }
}

val RegisteredDirectives.constraintsDumpFormats: List<ConstraintsDumpFormat>
    get() = this[FirDiagnosticsDirectives.DUMP_CONSTRAINTS]

val TestModule.kotlinFiles: List<TestFile>
    get() = files.filter { it.isKtFile || it.isKtsFile }
