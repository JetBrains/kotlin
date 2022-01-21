/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import java.io.File

internal val KotlinCoreEnvironment.destDir: File?
    get() = configuration.get(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY)

internal fun runCommonAnalysisForSerialization(
    environment: KotlinCoreEnvironment,
    dependOnBuiltins: Boolean,
    dependencyContainer: CommonDependenciesContainer?
): AnalyzerWithCompilerReport? {
    if (environment.destDir == null) {
        val configuration = environment.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        messageCollector.report(CompilerMessageSeverity.ERROR, "Specify destination via -d")
        return null
    }

    var analyzer: AnalyzerWithCompilerReport
    do {
        analyzer = runCommonAnalysis(environment, dependOnBuiltins, dependencyContainer)
        val result = analyzer.analysisResult
        if (result is AnalysisResult.RetryWithAdditionalRoots) {
            environment.addKotlinSourceRoots(result.additionalKotlinRoots)
        }
    } while (result is AnalysisResult.RetryWithAdditionalRoots)

    return if (analyzer.analysisResult.shouldGenerateCode) analyzer else null
}

private fun runCommonAnalysis(
    environment: KotlinCoreEnvironment,
    dependOnBuiltins: Boolean,
    dependencyContainer: CommonDependenciesContainer?
): AnalyzerWithCompilerReport {
    val configuration = environment.configuration
    val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val files = environment.getSourceFiles()
    val moduleName = Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>")

    val analyzer = AnalyzerWithCompilerReport(
        messageCollector,
        configuration.languageVersionSettings,
        configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    )

    analyzer.analyzeAndReport(files) {
        CommonResolverForModuleFactory.analyzeFiles(
            files, moduleName, dependOnBuiltins, configuration.languageVersionSettings,
            CommonPlatforms.defaultCommonPlatform, CompilerEnvironment,
            dependenciesContainer = dependencyContainer
        ) { content ->
            environment.createPackagePartProvider(content.moduleContentScope)
        }
    }

    return analyzer
}
