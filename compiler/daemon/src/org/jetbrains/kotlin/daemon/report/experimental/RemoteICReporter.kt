/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report.experimental

import kotlinx.coroutines.async
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilationResultsAsync
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBaseAsync
import org.jetbrains.kotlin.daemon.common.impls.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.impls.ReportCategory
import org.jetbrains.kotlin.daemon.common.impls.ReportSeverity
import org.jetbrains.kotlin.daemon.common.report
import org.jetbrains.kotlin.incremental.ICReporter
import java.io.File
import java.io.Serializable

internal class RemoteICReporterAsync(
    private val servicesFacade: CompilerServicesFacadeBaseAsync,
    private val compilationResults: CompilationResultsAsync?,
    compilationOptions: CompilationOptions
) : ICReporter {
    private val shouldReportMessages = ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories
    private val isVerbose = compilationOptions.reportSeverity == ReportSeverity.DEBUG.code
    private val shouldReportCompileIteration =
        CompilationResultCategory.IC_COMPILE_ITERATION.code in compilationOptions.requestedCompilationResults

    override fun report(message: () -> String) {
        async {
            if (shouldReportMessages && isVerbose) {
                servicesFacade.report(ReportCategory.IC_MESSAGE, ReportSeverity.DEBUG, message())
            }
        }
    }

    override fun reportCompileIteration(sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (shouldReportCompileIteration) {
            async {
                compilationResults?.add(
                    CompilationResultCategory.IC_COMPILE_ITERATION.code,
                    CompileIterationResult(sourceFiles, exitCode.toString())
                )
            }
        }
    }
}

class CompileIterationResult(
    @Suppress("unused") // used in Gradle
    val sourceFiles: Iterable<File>,
    @Suppress("unused") // used in Gradle
    val exitCode: String
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}