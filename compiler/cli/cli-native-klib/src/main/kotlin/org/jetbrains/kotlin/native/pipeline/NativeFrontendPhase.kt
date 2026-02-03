/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForLt
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForPsi
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.isCommonSourceForLt
import org.jetbrains.kotlin.cli.common.isCommonSourceForPsi
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.prepareNativeSessions
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.toVfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.FrontendFilesForPluginsGenerationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirBinaryDependenciesModuleData
import org.jetbrains.kotlin.fir.FirModuleCapabilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.SingleModuleFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.buildResolveAndCheckFirViaLightTree
import org.jetbrains.kotlin.fir.pipeline.runPlatformCheckers
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ImplicitIntegerCoercionModuleCapability
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.NativePhaseContext
import org.jetbrains.kotlin.native.createNativeKlibConfig

object NativeFrontendPhase : PipelinePhase<NativeConfigurationArtifact, NativeFrontendArtifact>(
    name = "NativeFrontendPhase",
    preActions = setOf(PerformanceNotifications.AnalysisStarted),
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeConfigurationArtifact): NativeFrontendArtifact? {
        val (configuration, environment, diagnosticCollector) = input
        val config = createNativeKlibConfig(configuration)
        val phaseContext = NativePhaseContext(config)
        val firOutput = phaseContext.firFrontend(environment)
        return NativeFrontendArtifact(
            firOutput,
            configuration = configuration,
            environment = environment,
            diagnosticCollector = diagnosticCollector,
            phaseContext = phaseContext,
        )
    }

    @OptIn(SessionConfiguration::class, ExperimentalCompilerApi::class)
    private inline fun <F> PhaseContext.firFrontend(
        input: KotlinCoreEnvironment,
        files: List<F>,
        fileHasSyntaxErrors: (F) -> Boolean,
        noinline isCommonSource: (F) -> Boolean,
        noinline fileBelongsToModule: (F, String) -> Boolean,
        buildResolveAndCheckFir: (FirSession, List<F>, BaseDiagnosticsCollector) -> SingleModuleFrontendOutput,
    ): AllModulesFrontendOutput {
        val configuration = input.configuration
        val extensionRegistrars = configuration.getCompilerExtensions(FirExtensionRegistrar)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
        val renderDiagnosticNames = configuration.renderDiagnosticInternalName

        // FIR
        val mainModuleName = Name.special("<${config.moduleId}>")
        val syntaxErrors = files.fold(false) { errorsFound, file -> fileHasSyntaxErrors(file) or errorsFound }
        val dependencyList = DependencyListForCliModule.build {
            val (interopLibs, regularLibs) = config.resolvedLibraries.getFullList().partition { it.isCInteropLibrary() }
            defaultDependenciesSet(mainModuleName) {
                dependencies(regularLibs.map { it.libraryFile.absolutePath })
                friendDependencies(config.friendModuleFiles.map { it.absolutePath })
                dependsOnDependencies(config.refinesModuleFiles.map { it.absolutePath })
            }
            if (interopLibs.isNotEmpty()) {
                val interopModuleData =
                    FirBinaryDependenciesModuleData(Name.special("<regular interop dependencies of $mainModuleName>"),
                                                    FirModuleCapabilities.create(listOf(ImplicitIntegerCoercionModuleCapability))
                    )
                dependencies(interopModuleData, interopLibs.map { it.libraryFile.absolutePath })
            }
            // TODO: !!! dependencies module data?
        }
        val resolvedLibraries = config.resolvedLibraries.getFullResolvedList().map { it.library }

        val sessionsWithSources = prepareNativeSessions(
            files,
            configuration,
            mainModuleName,
            resolvedLibraries,
            dependencyList,
            extensionRegistrars,
            metadataCompilationMode = config.metadataKlib,
            isCommonSource = isCommonSource,
            fileBelongsToModule = fileBelongsToModule,
        )

        val outputs = sessionsWithSources.map { (session, sources) ->
            buildResolveAndCheckFir(session, sources, diagnosticsReporter).also {
                if (config.configuration.getBoolean(KonanConfigKeys.PRINT_FILES)) {
                    it.fir.forEach { file -> println(file.render()) }
                }
            }
        }

        outputs.runPlatformCheckers(diagnosticsReporter)
        // This seems rudimental, but it fixes the error reporting in the case of one-stage compilation
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        return if (syntaxErrors || diagnosticsReporter.hasErrors) {
            throw CompilationErrorException("Compilation failed: there were frontend errors")
        } else {
            AllModulesFrontendOutput(outputs)
        }
    }

    private fun PhaseContext.firFrontendWithPsi(input: KotlinCoreEnvironment): AllModulesFrontendOutput {
        val configuration = input.configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        // FIR

        val ktFiles = input.getSourceFiles()
        return firFrontend(
            input,
            ktFiles,
            fileHasSyntaxErrors = {
                AnalyzerWithCompilerReport.reportSyntaxErrors(it, messageCollector).isHasErrors
            },
            isCommonSource = isCommonSourceForPsi,
            fileBelongsToModule = fileBelongsToModuleForPsi,
            buildResolveAndCheckFir = { session, files, diagnosticsReporter ->
                buildResolveAndCheckFirFromKtFiles(session, files, diagnosticsReporter)
            },
        )
    }

    private fun PhaseContext.firFrontendWithLightTree(input: KotlinCoreEnvironment): AllModulesFrontendOutput {
        val configuration = input.configuration
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        // FIR

        val groupedSources = collectSources(
            configuration,
            input.toVfsBasedProjectEnvironment(),
            messageCollector
        )

        val ktSourceFiles = mutableListOf<KtSourceFile>().apply {
            addAll(groupedSources.commonSources)
            addAll(groupedSources.platformSources)
        }

        return firFrontend(
            input,
            ktSourceFiles,
            fileHasSyntaxErrors = { false },
            isCommonSource = { groupedSources.isCommonSourceForLt(it) },
            fileBelongsToModule = { file, it -> groupedSources.fileBelongsToModuleForLt(file, it) },
            buildResolveAndCheckFir = { session, files, diagnosticsReporter ->
                buildResolveAndCheckFirViaLightTree(session, files, diagnosticsReporter, null)
            },
        )
    }

    private fun PhaseContext.firFrontend(input: KotlinCoreEnvironment): AllModulesFrontendOutput {
        var output = if (input.configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)) {
            firFrontendWithLightTree(input)
        } else {
            firFrontendWithPsi(input)
        }
        output = FrontendFilesForPluginsGenerationPipelinePhase.createFilesWithGeneratedDeclarations(output)
        return output
    }
}
