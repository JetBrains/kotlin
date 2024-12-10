/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.klib.compileModuleToAnalyzedFirWithPsi
import org.jetbrains.kotlin.cli.js.klib.compileModulesToAnalyzedFirWithLightTree
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.lookupTracker
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.checkers.JsStandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.WasmStandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.KotlinLibrary

object WebFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, WebFrontendPipelineArtifact>(
    name = "JsFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): WebFrontendPipelineArtifact? {
        val configuration = input.configuration
        val performanceManager = configuration.perfManager
        val environmentForJS = KotlinCoreEnvironment.createForProduction(input.rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        performanceManager?.notifyAnalysisStarted()
        val messageCollector = configuration.messageCollector
        val libraries = configuration.libraries
        val friendLibraries = configuration.friendLibraries

        val mainModule = MainModule.SourceFiles(environmentForJS.getSourceFiles())
        val moduleStructure = ModulesStructure(environmentForJS.project, mainModule, configuration, libraries, friendLibraries)

        val isWasm = configuration.wasmCompilation
        runStandardLibrarySpecialCompatibilityChecks(moduleStructure.allDependencies, isWasm = isWasm, messageCollector)

        val lookupTracker = configuration.lookupTracker ?: LookupTracker.DO_NOTHING

        performanceManager?.notifyAnalysisStarted()

        val kotlinPackageUsageIsFine: Boolean
        val analyzedOutput = if (configuration.useLightTree) {
            val groupedSources = collectSources(configuration, environmentForJS.project, messageCollector)

            if (
                groupedSources.isEmpty() &&
                !configuration.allowNoSourceFiles &&
                !configuration.jsIncrementalCompilationEnabled
            ) {
                if (!configuration.printVersion) {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "No source files")
                }
                return null
            }

            compileModulesToAnalyzedFirWithLightTree(
                moduleStructure = moduleStructure,
                groupedSources = groupedSources,
                // TODO: Only pass groupedSources, because
                //  we will need to have them separated again
                //  in createSessionsForLegacyMppProject anyway
                ktSourceFiles = groupedSources.commonSources + groupedSources.platformSources,
                libraries = libraries,
                friendLibraries = friendLibraries,
                diagnosticsReporter = input.diagnosticCollector,
                incrementalDataProvider = configuration.incrementalDataProvider,
                lookupTracker = lookupTracker,
                useWasmPlatform = isWasm,
            ).also {
                kotlinPackageUsageIsFine = it.output.all { checkKotlinPackageUsageForLightTree(configuration, it.fir) }
            }
        } else {
            val sourceFiles = environmentForJS.getSourceFiles()
            if (
                sourceFiles.isEmpty() &&
                !configuration.allowNoSourceFiles &&
                !configuration.jsIncrementalCompilationEnabled
            ) {
                if (!configuration.printVersion) {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "No source files")
                }
                return null
            }

            kotlinPackageUsageIsFine = checkKotlinPackageUsageForPsi(configuration, sourceFiles)
            compileModuleToAnalyzedFirWithPsi(
                moduleStructure = moduleStructure,
                ktFiles = sourceFiles,
                libraries = libraries,
                friendLibraries = friendLibraries,
                diagnosticsReporter = input.diagnosticCollector,
                incrementalDataProvider = configuration.incrementalDataProvider,
                lookupTracker = lookupTracker,
                useWasmPlatform = isWasm,
            )
        }

        if (!kotlinPackageUsageIsFine) return null

        performanceManager?.notifyAnalysisFinished()
        return WebFrontendPipelineArtifact(
            analyzedOutput,
            configuration,
            input.diagnosticCollector,
            moduleStructure,
        )
    }

    private fun runStandardLibrarySpecialCompatibilityChecks(
        libraries: List<KotlinLibrary>,
        isWasm: Boolean,
        messageCollector: MessageCollector,
    ) {
        val checker = if (isWasm) WasmStandardLibrarySpecialCompatibilityChecker else JsStandardLibrarySpecialCompatibilityChecker
        checker.check(libraries, messageCollector)
    }
}
