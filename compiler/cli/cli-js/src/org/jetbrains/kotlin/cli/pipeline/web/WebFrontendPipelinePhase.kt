/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.lookupTracker
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.useLightTree
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.session.KlibIcData
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.checkers.JsStandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.WasmStandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.nio.file.Paths

object WebFrontendPipelinePhase : PipelinePhase<ConfigurationPipelineArtifact, WebFrontendPipelineArtifact>(
    name = "JsFrontendPipelinePhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: ConfigurationPipelineArtifact): WebFrontendPipelineArtifact? {
        val configuration = input.configuration
        val environmentForJS = KotlinCoreEnvironment.createForProduction(input.rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        configuration.perfManager?.notifyAnalysisStarted()
        val messageCollector = configuration.messageCollector
        val libraries = configuration.libraries
        val friendLibraries = configuration.friendLibraries

        val mainModule = MainModule.SourceFiles(environmentForJS.getSourceFiles())
        val moduleStructure = ModulesStructure(environmentForJS.project, mainModule, configuration, libraries, friendLibraries)

        val isWasm = configuration.wasmCompilation
        runStandardLibrarySpecialCompatibilityChecks(moduleStructure.allDependencies, isWasm = isWasm, messageCollector)

        val lookupTracker = configuration.lookupTracker ?: LookupTracker.DO_NOTHING

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
                performanceManager = configuration.perfManager,
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

        return WebFrontendPipelineArtifact(
            analyzedOutput,
            configuration,
            input.diagnosticCollector,
            moduleStructure,
        )
    }

    fun compileModuleToAnalyzedFirWithPsi(
        moduleStructure: ModulesStructure,
        ktFiles: List<KtFile>,
        libraries: List<String>,
        friendLibraries: List<String>,
        diagnosticsReporter: BaseDiagnosticsCollector,
        incrementalDataProvider: IncrementalDataProvider?,
        lookupTracker: LookupTracker?,
        useWasmPlatform: Boolean,
    ): AnalyzedFirWithPsiOutput {
        for (ktFile in ktFiles) {
            AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, diagnosticsReporter)
        }
        val output = compileModuleToAnalyzedFir(
            moduleStructure,
            ktFiles,
            libraries,
            friendLibraries,
            incrementalDataProvider,
            lookupTracker,
            isCommonSource = isCommonSourceForPsi,
            fileBelongsToModule = fileBelongsToModuleForPsi,
            buildResolveAndCheckFir = { session, files ->
                buildResolveAndCheckFirFromKtFiles(session, files, diagnosticsReporter)
            },
            useWasmPlatform = useWasmPlatform,
        )
        output.runPlatformCheckers(diagnosticsReporter)
        return AnalyzedFirWithPsiOutput(output, ktFiles)
    }

    fun compileModulesToAnalyzedFirWithLightTree(
        moduleStructure: ModulesStructure,
        groupedSources: GroupedKtSources,
        ktSourceFiles: List<KtSourceFile>,
        libraries: List<String>,
        friendLibraries: List<String>,
        diagnosticsReporter: BaseDiagnosticsCollector,
        performanceManager: PerformanceManager?,
        incrementalDataProvider: IncrementalDataProvider?,
        lookupTracker: LookupTracker?,
        useWasmPlatform: Boolean,
    ): AnalyzedFirOutput {
        val output = compileModuleToAnalyzedFir(
            moduleStructure,
            ktSourceFiles,
            libraries,
            friendLibraries,
            incrementalDataProvider,
            lookupTracker,
            isCommonSource = { groupedSources.isCommonSourceForLt(it) },
            fileBelongsToModule = { file, it -> groupedSources.fileBelongsToModuleForLt(file, it) },
            buildResolveAndCheckFir = { session, files ->
                buildResolveAndCheckFirViaLightTree(session, files, diagnosticsReporter, performanceManager?.let { it::addSourcesStats })
            },
            useWasmPlatform = useWasmPlatform,
        )
        output.runPlatformCheckers(diagnosticsReporter)
        return AnalyzedFirOutput(output)
    }

    private inline fun <F> compileModuleToAnalyzedFir(
        moduleStructure: ModulesStructure,
        files: List<F>,
        libraries: List<String>,
        friendLibraries: List<String>,
        incrementalDataProvider: IncrementalDataProvider?,
        lookupTracker: LookupTracker?,
        noinline isCommonSource: (F) -> Boolean,
        noinline fileBelongsToModule: (F, String) -> Boolean,
        buildResolveAndCheckFir: (FirSession, List<F>) -> ModuleCompilerAnalyzedOutput,
        useWasmPlatform: Boolean,
    ): List<ModuleCompilerAnalyzedOutput> {
        // FIR
        val extensionRegistrars = FirExtensionRegistrar.getInstances(moduleStructure.project)

        val mainModuleName = moduleStructure.compilerConfiguration.get(CommonConfigurationKeys.MODULE_NAME)!!
        val escapedMainModuleName = Name.special("<$mainModuleName>")
        val platform = if (useWasmPlatform) {
            when (moduleStructure.compilerConfiguration.get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)) {
                WasmTarget.JS -> WasmPlatforms.wasmJs
                WasmTarget.WASI -> WasmPlatforms.wasmWasi
            }
        } else JsPlatforms.defaultJsPlatform
        val binaryModuleData = BinaryModuleData.initialize(escapedMainModuleName, platform)
        val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
            dependencies(libraries.map { Paths.get(it).toAbsolutePath() })
            friendDependencies(friendLibraries.map { Paths.get(it).toAbsolutePath() })
            // TODO: !!! dependencies module data?
        }

        val resolvedLibraries = moduleStructure.allDependencies

        val sessionsWithSources = if (useWasmPlatform) {
            prepareWasmSessions(
                files, moduleStructure.compilerConfiguration, escapedMainModuleName,
                resolvedLibraries, dependencyList, extensionRegistrars,
                isCommonSource = isCommonSource,
                fileBelongsToModule = fileBelongsToModule,
                lookupTracker,
                icData = incrementalDataProvider?.let(::KlibIcData),
            )
        } else {
            prepareJsSessions(
                files, moduleStructure.compilerConfiguration, escapedMainModuleName,
                resolvedLibraries, dependencyList, extensionRegistrars,
                isCommonSource = isCommonSource,
                fileBelongsToModule = fileBelongsToModule,
                lookupTracker,
                icData = incrementalDataProvider?.let(::KlibIcData),
            )
        }

        val outputs = sessionsWithSources.map {
            buildResolveAndCheckFir(it.session, it.files)
        }

        return outputs
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
