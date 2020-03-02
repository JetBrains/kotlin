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

import org.jetbrains.kotlin.build.report.RemoteBuildReporter
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.daemon.common.*
import java.util.*

fun getBuildReporter(
    servicesFacade: CompilerServicesFacadeBase,
    compilationResults: CompilationResults,
    compilationOptions: IncrementalCompilationOptions
): RemoteBuildReporter {
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
    for (requestedResult in requestedResults) {
        when (requestedResult) {
            CompilationResultCategory.IC_COMPILE_ITERATION -> {
                reporters.add(CompileIterationICReporter(compilationResults))
            }
            CompilationResultCategory.BUILD_REPORT_LINES -> {
                reporters.add(BuildReportICReporter(compilationResults, root))
            }
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES -> {
                reporters.add(BuildReportICReporter(compilationResults, root, isVerbose = true))
            }
        }
    }
    val areBuildMetricsNeeded = CompilationResultCategory.BUILD_METRICS in requestedResults
    val metricsReporter =
        (if (areBuildMetricsNeeded) BuildMetricsReporterImpl() else DoNothingBuildMetricsReporter)
            .let { RemoteBuildMetricsReporterAdapter(it, areBuildMetricsNeeded, compilationResults) }

    return RemoteBuildReporter(CompositeICReporter(reporters), metricsReporter)
}


