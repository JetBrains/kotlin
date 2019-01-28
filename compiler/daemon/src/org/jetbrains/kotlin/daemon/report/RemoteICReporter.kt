/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.ICReporter
import java.io.File

internal class RemoteICReporter(
    private val servicesFacade: CompilerServicesFacadeBase,
    private val compilationResults: CompilationResults,
    compilationOptions: IncrementalCompilationOptions
) : ICReporter {

    private val rootDir = compilationOptions.modulesInfo.projectRoot
    private val shouldReportMessages = ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories
    private val isVerbose = compilationOptions.reportSeverity == ReportSeverity.DEBUG.code
    private val shouldReportCompileIteration =
        CompilationResultCategory.IC_COMPILE_ITERATION.code in compilationOptions.requestedCompilationResults
    private val shouldReportICLog = CompilationResultCategory.IC_LOG.code in compilationOptions.requestedCompilationResults
    private val icLogLines = arrayListOf<String>()
    private val recompilationReason = HashMap<File, String>()

    override fun report(message: () -> String) {
        reportImpl(isMessageVerbose = false, message = message)
    }

    override fun reportVerbose(message: () -> String) {
        reportImpl(isMessageVerbose = true, message = message)
    }

    private fun reportImpl(isMessageVerbose: Boolean, message: () -> String) {
        val lazyMessage = lazy { message() }

        val shouldReportVerbose = isVerbose || !isMessageVerbose
        if (shouldReportMessages && shouldReportVerbose) {
            servicesFacade.report(ReportCategory.IC_MESSAGE, ReportSeverity.DEBUG, lazyMessage.value)
        }
        if (shouldReportICLog && shouldReportVerbose) {
            icLogLines.add(lazyMessage.value)
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (shouldReportCompileIteration) {
            compilationResults.add(
                CompilationResultCategory.IC_COMPILE_ITERATION.code,
                CompileIterationResult(sourceFiles, exitCode.toString())
            )
        }
        if (shouldReportICLog && incremental) {
            icLogLines.add("Compile iteration:")
            sourceFiles.relativePaths(rootDir).forEach { file ->
                val reason = recompilationReason[file]?.let { " <- $it" } ?: ""
                icLogLines.add("  $file$reason")
            }
            recompilationReason.clear()
        }
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        super.reportMarkDirty(affectedFiles, reason)
        if (shouldReportICLog) {
            affectedFiles.forEach { recompilationReason[it] = reason }
        }
    }

    fun flush() {
        if (shouldReportICLog) {
            compilationResults.add(CompilationResultCategory.IC_LOG.code, icLogLines)
        }
    }

    private fun File.relativeOrCanonical(base: File): String =
        relativeToOrNull(base)?.path ?: canonicalPath

    private fun Iterable<File>.relativePaths(base: File): List<String> =
        map { it.relativeOrCanonical(base) }.sorted()
}

