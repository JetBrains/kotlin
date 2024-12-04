/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics.file

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.*
import java.io.File

data class ReadableFileReportData<B : BuildTime, P : BuildPerformanceMetric>(
    val statisticsData: List<CompileStatisticsData<B, P>>,
    val startParameters: BuildStartParameters,
    val failureMessages: List<String> = emptyList(),
    val version: Int = 1
)

open class ReadableFileReportService<B : BuildTime, P : BuildPerformanceMetric>(
    buildReportDir: File,
    projectName: String,
    private val printMetrics: Boolean,
) : FileReportService<ReadableFileReportData<B, P>>(buildReportDir, projectName, "txt") {

    open fun printCustomTaskMetrics(statisticsData: CompileStatisticsData<B, P>, printer: Printer) {}

    /**
     * Prints general build information, sum up compile metrics and detailed task and transform information.
     *
     * BuildExecutionData / BuildOperationRecord contains data for both tasks and transforms.
     * We still use the term "tasks" because saying "tasks/transforms" is a bit verbose and "build operations" may sound a bit unfamiliar.
     */
    override fun printBuildReport(data: ReadableFileReportData<B, P>, outputFile: File) {
        outputFile.bufferedWriter().use { writer ->
            Printer(writer).printBuildReport(data, printMetrics) { compileStatisticsData ->
                printCustomTaskMetrics(
                    compileStatisticsData,
                    this
                )
            }
        }
    }
}