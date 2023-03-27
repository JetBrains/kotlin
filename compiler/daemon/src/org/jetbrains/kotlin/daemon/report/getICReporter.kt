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

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.RemoteBuildReporter
import org.jetbrains.kotlin.build.report.RemoteICReporter
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporterImpl
import org.jetbrains.kotlin.build.report.metrics.DoNothingBuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.daemon.common.*

fun getBuildReporter(
    servicesFacade: CompilerServicesFacadeBase,
    compilationResults: CompilationResults,
    compilationOptions: IncrementalCompilationOptions
): RemoteBuildReporter<GradleBuildTime, GradleBuildPerformanceMetric> {
    val root = compilationOptions.rootProjectDir
    val reporters = ArrayList<RemoteICReporter>()

    if (ReportCategory.IC_MESSAGE.code in compilationOptions.reportCategories) {
        reporters.add(
            DebugMessagesICReporter(
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
                reporters.add(CompileIterationICReporter(compilationResults))
            }
            CompilationResultCategory.BUILD_REPORT_LINES -> {
                reporters.add(BuildReportICReporter(compilationResults, root))
            }
            CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES -> {
                reporters.add(BuildReportICReporter(compilationResults, root, isVerbose = true))
            }
            CompilationResultCategory.BUILD_METRICS -> {
            }
        }
    }
    val areBuildMetricsNeeded = CompilationResultCategory.BUILD_METRICS in requestedResults
    val metricsReporter =
        (if (areBuildMetricsNeeded) BuildMetricsReporterImpl() else DoNothingBuildMetricsReporter)
            .let { RemoteBuildMetricsReporterAdapter(it, areBuildMetricsNeeded, compilationResults) }

    return RemoteBuildReporter(CompositeICReporter(reporters), metricsReporter)
}

internal fun ReportSeverity.getSeverity(

    /**
     * If true, [ReportSeverity.ERROR] will be mapped to [ICReporter.ReportSeverity.WARNING] (i.e., when [ReportSeverity.ERROR] is
     * specified, [ICReporter.ReportSeverity.WARNING] messages will also be shown).
     *
     * NOTE: This parameter exists only because we don't have `ICReporter.ReportSeverity.ERROR` yet. Once that we create that enum, we can
     * remove this parameter.
     */
    mapErrorToWarning: Boolean,

    /**
     * If true, [ReportSeverity.INFO] will be mapped to [ICReporter.ReportSeverity.WARNING] (i.e., when [ReportSeverity.INFO] is specified,
     * only messages with severity [ICReporter.ReportSeverity.WARNING] and above will be shown).
     *
     * NOTE: This parameter exists only to preserve the previous behavior (`mapInfoToWarning = true`). If `mapInfoToWarning` is never set
     * to `true`, we can remove this parameter.
     */
    mapInfoToWarning: Boolean

): ICReporter.ReportSeverity {
    return when (this) {
        ReportSeverity.ERROR -> if (mapErrorToWarning) {
            ICReporter.ReportSeverity.WARNING
        } else {
            throw IllegalArgumentException(
                "No mapping exists for `ReportSeverity.ERROR`." +
                        " Add `ICReporter.ReportSeverity.ERROR` and remove mapErrorToWarning, or set mapErrorToWarning = true."
            )
        }
        ReportSeverity.WARNING -> ICReporter.ReportSeverity.WARNING
        ReportSeverity.INFO -> if (mapInfoToWarning) {
            ICReporter.ReportSeverity.WARNING
        } else {
            ICReporter.ReportSeverity.INFO
        }
        ReportSeverity.DEBUG -> ICReporter.ReportSeverity.DEBUG
    }
}
