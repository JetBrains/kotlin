/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report.experimental

import kotlinx.coroutines.*
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.report.CompositeICReporter
import org.jetbrains.kotlin.daemon.report.RemoteICReporter
import org.jetbrains.kotlin.incremental.ICReporterBase
import java.io.File

internal class DebugMessagesICReporterAsync(
    private val servicesFacade: CompilerServicesFacadeBaseAsync,
    rootDir: File,
    private val isVerbose: Boolean
) : ICReporterBase(rootDir), RemoteICReporter {
    override fun report(message: () -> String) {
        GlobalScope.async {
            servicesFacade.report(
                ReportCategory.IC_MESSAGE,
                ReportSeverity.DEBUG, message()
            )
        }
    }

    override fun reportVerbose(message: () -> String) {
        if (isVerbose) {
            report(message)
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

    override fun report(message: () -> String) {
    }

    override fun reportVerbose(message: () -> String) {
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

    override fun report(message: () -> String) {
        icLogLines.add(message())
    }

    override fun reportVerbose(message: () -> String) {
        if (isVerbose) {
            report(message)
        }
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
): RemoteICReporter {
    val root = compilationOptions.modulesInfo.projectRoot
    val reporters = ArrayList<RemoteICReporter>()

    if (ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories) {
        val isVerbose = compilationOptions.reportSeverity == ReportSeverity.DEBUG.code
        reporters.add(DebugMessagesICReporterAsync(servicesFacade, root, isVerbose = isVerbose))
    }

    val requestedResults = compilationOptions
        .requestedCompilationResults
        .mapNotNullTo(HashSet()) { resultCode ->
            CompilationResultCategory.values().getOrNull(resultCode)
        }
    requestedResults.mapTo(reporters) { requestedResult ->
        when (requestedResult) {
            CompilationResultCategory.IC_COMPILE_ITERATION -> {
                CompileIterationICReporterAsync(compilationResults)
            }
            CompilationResultCategory.BUILD_REPORT_LINES -> {
                BuildReportICReporterAsync(compilationResults, root)
            }
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES -> {
                BuildReportICReporterAsync(compilationResults, root, isVerbose = true)
            }
        }
    }

    return CompositeICReporter(reporters)
}