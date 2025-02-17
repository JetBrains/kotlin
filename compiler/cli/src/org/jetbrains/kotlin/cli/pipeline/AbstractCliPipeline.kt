/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.invokeToplevel
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.forEachStringMeasurement
import java.io.File

abstract class AbstractCliPipeline<A : CommonCompilerArguments> {
    fun execute(
        arguments: A,
        services: Services,
        originalMessageCollector: MessageCollector,
    ): ExitCode {
        val canceledStatus = services[CompilationCanceledStatus::class.java]
        ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus)
        val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
        setIdeaIoUseFallback() // TODO (KT-73573): probably could be removed
        val performanceManager = createPerformanceManager(arguments, services).apply { isK2 = true }
        if (arguments.reportPerf || arguments.dumpPerf != null) {
            performanceManager.enableExtendedStats()
        }

        val messageCollector = GroupingMessageCollector(
            originalMessageCollector,
            arguments.allWarningsAsErrors,
            arguments.reportAllWarnings
        )
        val argumentsInput = ArgumentsPipelineArtifact(
            arguments,
            services,
            rootDisposable,
            messageCollector,
            performanceManager
        )

        fun reportException(e: Throwable): ExitCode {
            MessageCollectorUtil.reportException(messageCollector, e) // TODO (KT-73575): investigate reporting in case of OOM
            return if (e is OutOfMemoryError || e.hasOOMCause()) ExitCode.OOM_ERROR else ExitCode.INTERNAL_ERROR
        }

        fun reportCompilationCanceled(e: CompilationCanceledException): ExitCode {
            messageCollector.reportCompilationCancelled(e)
            return ExitCode.OK
        }

        return try {
            val code = runPhasedPipeline(argumentsInput)
            performanceManager.notifyCompilationFinished()
            if (arguments.reportPerf) {
                messageCollector.report(CompilerMessageSeverity.LOGGING, "PERF: " + performanceManager.getTargetInfo())
                performanceManager.unitStats.forEachStringMeasurement {
                    messageCollector.report(CompilerMessageSeverity.LOGGING, "PERF: $it", null)
                }
            }

            if (arguments.dumpPerf != null) {
                performanceManager.dumpPerformanceReport(File(arguments.dumpPerf!!))
            }

            if (messageCollector.hasErrors()) ExitCode.COMPILATION_ERROR else code
        } catch (_: CompilationErrorException) {
            ExitCode.COMPILATION_ERROR
        } catch (e: RuntimeException) {
            when (val cause = e.cause) {
                is CompilationCanceledException -> reportCompilationCanceled(cause)
                else -> reportException(e)
            }
        } catch (t: Throwable) {
            reportException(t)
        } finally {
            messageCollector.flush()
            disposeRootInWriteAction(rootDisposable)
        }
    }

    private fun runPhasedPipeline(input: ArgumentsPipelineArtifact<A>): ExitCode {
        val compoundPhase = createCompoundPhase(input.arguments)

        val phaseConfig = PhaseConfig()
        val context = PipelineContext(
            input.messageCollector,
            input.diagnosticCollector,
            input.performanceManager,
            renderDiagnosticInternalName = input.arguments.renderInternalDiagnosticNames,
            kaptMode = isKaptMode(input.arguments)
        )
        return try {
            val result = compoundPhase.invokeToplevel(
                phaseConfig,
                context,
                input
            )
            when (result) {
                is PipelineArtifactWithExitCode -> result.exitCode
                else -> ExitCode.OK
            }
        } catch (e: PipelineStepException) {
            /**
             * There might be a case when the pipeline is not executed fully, but it's not considered as a compilation error:
             *   if `-version` flag was passed
             */
            if (e.definitelyCompilationError || input.messageCollector.hasErrors() || input.diagnosticCollector.hasErrors) {
                ExitCode.COMPILATION_ERROR
            } else {
                ExitCode.OK
            }
        } catch (_: SuccessfulPipelineExecutionException) {
            ExitCode.OK
        } finally {
            CheckCompilationErrors.CheckDiagnosticCollector.reportDiagnosticsToMessageCollector(context)
        }
    }

    abstract fun createCompoundPhase(arguments: A): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<A>, *>
    abstract val defaultPerformanceManager: PerformanceManager

    /**
     * Some CLIs might support non-standard performance managers, so this method is needed to be able to create such a manager if needed.
     */
    protected open fun createPerformanceManager(arguments: A, services: Services): PerformanceManager {
        return defaultPerformanceManager
    }

    protected open fun isKaptMode(arguments: A): Boolean = false
}
