/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report.experimental

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.ICReporterBase
import org.jetbrains.kotlin.build.report.RemoteBuildReporter
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.RemoteBuildMetricsReporter
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.report.CompositeICReporter
import org.jetbrains.kotlin.daemon.report.getSeverity
import java.io.File

internal class DebugMessagesICReporterAsync(
    private val servicesFacade: CompilerServicesFacadeBaseAsync,
    rootDir: File,
    private val reportSeverity: ICReporter.ReportSeverity
) : ICReporterBase(rootDir), RemoteICReporter {

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
        if (severity.level < reportSeverity.level) return

        GlobalScope.async {
            servicesFacade.report(ReportCategory.IC_MESSAGE, severity.getSeverity(), message())
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
    }

    override fun flush() {
    }
}

internal class CompileIterationICReporterAsync(
    private val compilationResults: CompilationResultsAsync?
) : ICReporterBase(), RemoteICReporter {
    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        GlobalScope.async {
            compilationResults?.add(
                CompilationResultCategory.IC_COMPILE_ITERATION.code,
                CompileIterationResult(sourceFiles, exitCode.toString())
            )
        }
    }

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
    }

    override fun flush() {
    }
}

internal class BuildReportICReporterAsync(
    private val compilationResults: CompilationResultsAsync?,
    rootDir: File,
    private val isVerbose: Boolean = false
) : ICReporterBase(rootDir), RemoteICReporter {
    private val icLogLines = arrayListOf<String>()
    private val recompilationReason = HashMap<File, String>()

    override fun report(message: () -> String, severity: ICReporter.ReportSeverity) {
        if (severity == ICReporter.ReportSeverity.DEBUG && !isVerbose) return

        icLogLines.add(message())
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (!incremental) return

        icLogLines.add("Compile iteration:")
        for (file in sourceFiles) {
            val reason = recompilationReason[file]?.let { " <- $it" } ?: ""
            icLogLines.add("  ${file.relativeOrCanonical()}$reason")
        }
        recompilationReason.clear()
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        affectedFiles.forEach { recompilationReason[it] = reason }
    }

    override fun flush() {
        GlobalScope.async {
            compilationResults?.add(CompilationResultCategory.BUILD_REPORT_LINES.code, icLogLines)
        }
    }
}

fun getICReporterAsync(
    servicesFacade: CompilerServicesFacadeBaseAsync,
    compilationResults: CompilationResultsAsync?,
    compilationOptions: IncrementalCompilationOptions
): RemoteBuildReporter {
    val root = compilationOptions.modulesInfo.projectRoot
    val reporters = ArrayList<RemoteICReporter>()

    if (ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories) {
        reporters.add(
            DebugMessagesICReporterAsync(
                servicesFacade = servicesFacade,
                rootDir = root,
                reportSeverity = ReportSeverity.fromCode(compilationOptions.reportSeverity)
                    .getSeverity(mapErrorToWarning = true, mapInfoToWarning = true)
            )
        )
    }

    val requestedResults = compilationOptions
        .requestedCompilationResults
        .mapNotNullTo(HashSet()) { resultCode ->
            CompilationResultCategory.values().getOrNull(resultCode)
        }
    for (requestedResult in requestedResults) {
        when (requestedResult) {
            CompilationResultCategory.IC_COMPILE_ITERATION -> {
                reporters.add(CompileIterationICReporterAsync(compilationResults))
            }
            CompilationResultCategory.BUILD_REPORT_LINES -> {
                reporters.add(BuildReportICReporterAsync(compilationResults, root))
            }
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES -> {
                reporters.add(BuildReportICReporterAsync(compilationResults, root, isVerbose = true))
            }
            CompilationResultCategory.BUILD_METRICS -> {
            }
        }
    }

    val icReporter = CompositeICReporter(reporters)
    val metricsReporter = DoNothingRemoteBuildMetricsReporter
    return RemoteBuildReporter(icReporter, metricsReporter)
}

object DoNothingRemoteBuildMetricsReporter : BuildMetricsReporter by DoNothingBuildMetricsReporter, RemoteBuildMetricsReporter {
    override fun flush() {
    }
}
