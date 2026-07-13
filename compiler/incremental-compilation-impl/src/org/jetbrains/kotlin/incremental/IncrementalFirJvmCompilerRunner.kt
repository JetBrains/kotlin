/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.reportToMessageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.FrontendFilesForPluginsGenerationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelineStepException
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmConfigurationPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmWriteOutputsPhase
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import java.io.File

open class IncrementalFirJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<BuildTimeMetric, BuildPerformanceMetric>,
    outputDirs: Collection<File>?,
    classpathChanges: ClasspathChanges,
    kotlinSourceFilesExtensions: Set<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
    generateCompilerRefIndex: Boolean = false,
    val compilationCanceledStatus: CompilationCanceledStatus? = null,
    override val lookupTrackerDelegate: LookupTracker = LookupTracker.DO_NOTHING,
) : IncrementalJvmCompilerRunner(
    workingDir,
    reporter,
    outputDirs,
    classpathChanges,
    kotlinSourceFilesExtensions,
    icFeatures,
    generateCompilerRefIndex,
    compilationCanceledStatus = compilationCanceledStatus
) {
    private val configurationPhase = JvmConfigurationPipelinePhase
    private val frontendPhases = JvmFrontendPipelinePhase then
            FrontendFilesForPluginsGenerationPipelinePhase() then
            JvmFir2IrPipelinePhase
    private val backendPhases = JvmBackendPipelinePhase then
            JvmWriteOutputsPhase

    override fun runCompiler(
        sourcesToCompile: List<File>,
        args: K2JVMCompilerArguments,
        caches: IncrementalJvmCachesManager,
        services: Services,
        messageCollector: MessageCollector,
        allSources: List<File>,
        isIncremental: Boolean
    ): Pair<ExitCode, Collection<File>> {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "The FIR runner is deprecated and will be removed in Kotlin 2.5.0."
        )
        ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(compilationCanceledStatus)
        val rootDisposable = Disposer.newDisposable("Disposable for ${IncrementalFirJvmCompilerRunner::class.simpleName}.runCompiler")
        val collector = GroupingMessageCollector(
            messageCollector,
            args.allWarningsAsErrors,
            args.reportAllWarnings
        )
        val performanceManager = createPerformanceManagerFor(JvmPlatforms.defaultJvmPlatform)
        if (args.reportPerf || args.dumpPerf != null) {
            performanceManager.enableExtendedStats()
        }

        val confPhaseArgs = ArgumentsPipelineArtifact(
            args,
            services,
            rootDisposable,
            collector,
            performanceManager
        )
        val baseCompilationConfiguration = configurationPhase.executePhase(confPhaseArgs)
        val baseContentRootsWithoutKotlin = baseCompilationConfiguration.configuration[CLIConfigurationKeys.CONTENT_ROOTS]
            ?.filterNot { it is KotlinSourceRoot }
            ?: emptyList()

        @OptIn(PipelineArtifact.CliPipelineInternals::class)
        val incrementalCompilationConfiguration = baseCompilationConfiguration.withCompilerConfiguration(
            baseCompilationConfiguration.configuration.copy().apply {
                // JVMConfigurationKeys.MODULES and JVMConfigurationKeys.MODULE_CHUNK seem to be unrelevant
                // and generally not used in the new pipeline
                val hmppCliModuleStructure = get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
                val kotlinContentRoots = sourcesToCompile.map {
                    val kotlinSourceFilePath = it.path
                    KotlinSourceRoot(
                        path = kotlinSourceFilePath,
                        isCommon = hmppCliModuleStructure?.isFromCommonModule(kotlinSourceFilePath) ?: false,
                        hmppModuleName = hmppCliModuleStructure?.getModuleNameForSource(kotlinSourceFilePath)
                    )
                }
                put(CLIConfigurationKeys.CONTENT_ROOTS, baseContentRootsWithoutKotlin + kotlinContentRoots)
                // We want to regenerate .kotlin_module if file was deleted
                put(CLIConfigurationKeys.ALLOW_NO_SOURCE_FILES, true)
            }
        )

        try {
            val phasedConfig = PhaseConfig()
            val context = PipelineContext(
                performanceManager,
                kaptMode = isKaptMode(args)
            )
            val compilationOutput = frontendPhases.invokeToplevel(
                phasedConfig,
                context,
                incrementalCompilationConfiguration
            )

            if (compilationOutput.configuration.diagnosticsCollector.hasErrors) {
                compilationOutput.configuration.diagnosticsCollector.reportToMessageCollector(
                    messageCollector,
                    renderDiagnosticName = compilationOutput.configuration.renderDiagnosticInternalName
                )
                return ExitCode.COMPILATION_ERROR to sourcesToCompile
            }

            val backendOutput = backendPhases.invokeToplevel(phasedConfig, context, compilationOutput)
            backendOutput.configuration.diagnosticsCollector.reportToMessageCollector(
                messageCollector,
                renderDiagnosticName = backendOutput.configuration.renderDiagnosticInternalName
            )
        } catch (_: PipelineStepException) {
            if (CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(incrementalCompilationConfiguration.configuration)) {
                CheckCompilationErrors.CheckDiagnosticCollector.reportToMessageCollector(incrementalCompilationConfiguration.configuration)
            }
            return ExitCode.COMPILATION_ERROR to sourcesToCompile
        } catch (_: CompilationCanceledException) {
            collector.report(CompilerMessageSeverity.INFO, "Compilation was canceled", null)
            return ExitCode.OK to sourcesToCompile
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is CompilationCanceledException) {
                collector.report(CompilerMessageSeverity.INFO, "Compilation was canceled", null)
                return ExitCode.OK to sourcesToCompile
            } else {
                throw e
            }
        } finally {
            collector.flush()
            Disposer.dispose(rootDisposable)
        }
        return ExitCode.OK to sourcesToCompile
    }
}

// copied from JvmCliPipeline, potentially should be unified within one place to get the flag
private fun isKaptMode(arguments: K2JVMCompilerArguments): Boolean {
    return arguments.pluginOptions.any { it.startsWith("plugin:org.jetbrains.kotlin.kapt3") }
}
