/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.daemon.incremental

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.report.FilteringReporterBase
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationServicesFacade
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.incremental.IncrementalCompilationSeverity.COMPILED_FILES
import org.jetbrains.kotlin.daemon.incremental.IncrementalCompilationSeverity.LOGGING
import org.jetbrains.kotlin.incremental.ICReporter
import java.io.File
import java.io.Serializable

internal class RemoteICReporter(
        servicesFacade: CompilerServicesFacadeBase,
        additionalCompilerArgs: CompilationOptions
) : FilteringReporterBase(servicesFacade, additionalCompilerArgs, ReportCategory.INCREMENTAL_COMPILATION), ICReporter {
    override fun report(message: () -> String) {
        if (shouldReport(LOGGING.value)) {
            report(LOGGING.value, message())
        }
    }

    override fun reportCompileIteration(sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (shouldReport(COMPILED_FILES.value)) {
            report(COMPILED_FILES.value, message = null, attachment = CompileIterationResult(sourceFiles, exitCode.toString()))
        }
    }
}

/**
 * See [RemoteICReporter]
 */
enum class IncrementalCompilationSeverity(val value: Int) {
    COMPILED_FILES(0),
    LOGGING(10)
}

class CompileIterationResult(val sourceFiles: Iterable<File>, val exitCode: String) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}