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
import java.io.Serializable

internal class RemoteICReporter(
        private val servicesFacade: CompilerServicesFacadeBase,
        private val compilationResults: CompilationResults,
        compilationOptions: CompilationOptions
) : ICReporter {
    private val shouldReportMessages = ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories
    private val isVerbose = compilationOptions.reportSeverity == ReportSeverity.DEBUG.code
    private val shouldReportCompileIteration = CompilationResultCategory.IC_COMPILE_ITERATION.code in compilationOptions.requestedCompilationResults

    override fun report(message: () -> String) {
        if (shouldReportMessages && isVerbose) {
            servicesFacade.report(ReportCategory.IC_MESSAGE, ReportSeverity.DEBUG, message())
        }
    }

    override fun reportCompileIteration(sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (shouldReportCompileIteration) {
            compilationResults.add(CompilationResultCategory.IC_COMPILE_ITERATION.code, CompileIterationResult(sourceFiles, exitCode.toString()))
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