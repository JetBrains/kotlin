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

import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.util.*

internal fun getICReporter(
    servicesFacade: CompilerServicesFacadeBase,
    compilationResults: CompilationResults,
    compilationOptions: IncrementalCompilationOptions
): RemoteICReporter {
    val root = compilationOptions.modulesInfo.projectRoot
    val reporters = ArrayList<RemoteICReporter>()

    if (ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories) {
        val isVerbose = compilationOptions.reportSeverity == ReportSeverity.DEBUG.code
        reporters.add(DebugMessagesICReporter(servicesFacade, root, isVerbose = isVerbose))
    }

    val requestedResults = compilationOptions
        .requestedCompilationResults
        .mapNotNullTo(HashSet()) { resultCode ->
            CompilationResultCategory.values().getOrNull(resultCode)
        }
    requestedResults.mapTo(reporters) { requestedResult ->
        when (requestedResult) {
            CompilationResultCategory.IC_COMPILE_ITERATION -> {
                CompileIterationICReporter(compilationResults)
            }
            CompilationResultCategory.BUILD_REPORT_LINES -> {
                BuildReportICReporter(compilationResults, root)
            }
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES -> {
                BuildReportICReporter(compilationResults, root, isVerbose = true)
            }
        }
    }

    return CompositeICReporter(reporters)
}


