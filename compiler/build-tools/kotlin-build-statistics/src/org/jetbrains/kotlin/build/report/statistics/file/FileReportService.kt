/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics.file

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.*
import org.jetbrains.kotlin.build.report.statistics.asString
import org.jetbrains.kotlin.build.report.statistics.formatTime
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

open class FileReportService<B : BuildTime, P : BuildPerformanceMetric>(
    buildReportDir: File,
    projectName: String,
    private val printMetrics: Boolean,
    private val logger: KotlinLogger,
) : Serializable {
    companion object {
        private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").also { it.timeZone = TimeZone.getTimeZone("UTC") }
    }
    private val ts = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().time)
    private val outputFile = buildReportDir.resolve("$projectName-build-$ts.txt")

    protected lateinit var p: Printer

    open fun printCustomTaskMetrics(statisticsData: CompileStatisticsData<B, P>) {}

    fun process(
        statisticsData: List<CompileStatisticsData<B, P>>,
        startParameters: BuildStartParameters,
        failureMessages: List<String> = emptyList(),
    ) {
        val buildReportPath = outputFile.toPath().toUri().toString()
        try {
            outputFile.parentFile.mkdirs()
            if (!(outputFile.parentFile.exists() && outputFile.parentFile.isDirectory)) {
                logger.error("Kotlin build report cannot be created: '$outputFile.parentFile' is a file or do not have permissions to create")
                return
            }

            outputFile.bufferedWriter().use { writer ->
                p = Printer(writer)
                printBuildReport(statisticsData, startParameters, failureMessages)
            }

            logger.lifecycle("Kotlin build report is written to $buildReportPath")
        } catch (e: Exception) {
            logger.error("Could not write Kotlin build report to $buildReportPath", e)
        }
    }

    private fun printBuildReport(
        statisticsData: List<CompileStatisticsData<B, P>>,
        startParameters: BuildStartParameters,
        failureMessages: List<String>,
    ) {
        // NOTE: BuildExecutionData / BuildOperationRecord contains data for both tasks and transforms.
        // Where possible, we still use the term "tasks" because saying "tasks/transforms" is a bit verbose and "build operations" may sound
        // a bit unfamiliar.
        // TODO: If it is confusing, consider renaming "tasks" to "build operations" in this class.
        printBuildInfo(startParameters, failureMessages)
        if (printMetrics && statisticsData.isNotEmpty()) {
            printMetrics(
                statisticsData.map { it.getBuildTimesMetrics() }.reduce { agg, value ->
                    (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
                },
                statisticsData.map { it.getPerformanceMetrics() }.reduce { agg, value ->
                    (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
                },
                statisticsData.map { it.getNonIncrementalAttributes().asSequence() }.reduce { agg, value -> agg + value }.toList(),
                aggregatedMetric = true
            )
            p.println()
        }
        printTaskOverview(statisticsData)
        printTasksLog(statisticsData)
    }

    private fun printBuildInfo(startParameters: BuildStartParameters, failureMessages: List<String>) {
        p.withIndent("Gradle start parameters:") {
            startParameters.let {
                p.println("tasks = ${it.tasks}")
                p.println("excluded tasks = ${it.excludedTasks}")
                p.println("current dir = ${it.currentDir}")
                p.println("project properties args = ${it.projectProperties}")
                p.println("system properties args = ${it.systemProperties}")
            }
        }
        p.println()

        if (failureMessages.isNotEmpty()) {
            p.println("Build failed: ${failureMessages}")
            p.println()
        }
    }

    private fun printMetrics(
        buildTimesMetrics: Map<out BuildTime, Long>,
        performanceMetrics: Map<out BuildPerformanceMetric, Long>,
        nonIncrementalAttributes: Collection<BuildAttribute>,
        gcTimeMetrics: Map<String, Long>? = emptyMap(),
        gcCountMetrics: Map<String, Long>? = emptyMap(),
        aggregatedMetric: Boolean = false,
    ) {
        printBuildTimes(buildTimesMetrics)
        if (aggregatedMetric) p.println()

        printBuildPerformanceMetrics(performanceMetrics)
        if (aggregatedMetric) p.println()

        printBuildAttributes(nonIncrementalAttributes)

        //TODO: KT-57310 Implement build GC metric in
        if (!aggregatedMetric) {
            printGcMetrics(gcTimeMetrics, gcCountMetrics)
        }
    }

    private fun printGcMetrics(
        gcTimeMetrics: Map<String, Long>?,
        gcCountMetrics: Map<String, Long>?,
    ) {
        val keys = HashSet<String>()
        gcCountMetrics?.keys?.also { keys.addAll(it) }
        gcTimeMetrics?.keys?.also { keys.addAll(it) }
        if (keys.isEmpty()) return

        p.withIndent("GC metrics:") {
            for (key in keys) {
                p.println("$key:")
                p.withIndent {
                    gcCountMetrics?.get(key)?.also { p.println("GC count: ${it}") }
                    gcTimeMetrics?.get(key)?.also { p.println("GC time: ${formatTime(it)}") }
                }
            }
        }
    }

    private fun printBuildTimes(buildTimes: Map<out BuildTime, Long>) {
        if (buildTimes.isEmpty()) return

        p.println("Time metrics:")
        p.withIndent {
            val visitedBuildTimes = HashSet<BuildTime>()
            fun printBuildTime(buildTime: BuildTime) {
                if (!visitedBuildTimes.add(buildTime)) return

                val timeMs = buildTimes[buildTime]
                if (timeMs != null) {
                    p.println("${buildTime.getReadableString()}: ${formatTime(timeMs)}")
                    p.withIndent {
                        buildTime.children()?.forEach { printBuildTime(it) }
                    }
                } else {
                    //Skip formatting if parent metric does not set
                    buildTime.children()?.forEach { printBuildTime(it) }
                }
            }

            for (buildTime in buildTimes.keys.first().getAllMetrics()) {
                if (buildTime.getParent() != null) continue

                printBuildTime(buildTime)
            }
        }
    }

    private fun printBuildPerformanceMetrics(buildMetrics: Map<out BuildPerformanceMetric, Long>) {
        if (buildMetrics.isEmpty()) return

        p.withIndent("Size metrics:") {
            for (metric in buildMetrics.keys.first().getAllMetrics()) {
                buildMetrics[metric]?.let { printSizeMetric(metric, it) }
            }
        }
    }

    private fun printSizeMetric(sizeMetric: BuildPerformanceMetric, value: Long) {
        fun BuildPerformanceMetric.numberOfAncestors(): Int {
            var count = 0
            var parent: BuildPerformanceMetric? = getParent()
            while (parent != null) {
                count++
                parent = parent.getParent()
            }
            return count
        }

        val indentLevel = sizeMetric.numberOfAncestors()

        repeat(indentLevel) { p.pushIndent() }
        when (sizeMetric.getType()) {
            ValueType.BYTES -> p.println("${sizeMetric.getReadableString()}: ${formatSize(value)}")
            ValueType.NUMBER -> p.println("${sizeMetric.getReadableString()}: $value")
            ValueType.NANOSECONDS -> p.println("${sizeMetric.getReadableString()}: $value")
            ValueType.MILLISECONDS -> p.println("${sizeMetric.getReadableString()}: ${formatTime(value)}")
            ValueType.TIME -> p.println("${sizeMetric.getReadableString()}: ${formatter.format(value)}")
        }
        repeat(indentLevel) { p.popIndent() }
    }

    private fun printBuildAttributes(buildAttributes: Collection<BuildAttribute>) {
        if (buildAttributes.isEmpty()) return

        val buildAttributesMap = buildAttributes.groupingBy { it }.eachCount()
        p.withIndent("Build attributes:") {
            val attributesByKind = buildAttributesMap.entries.groupBy { it.key.kind }.toSortedMap()
            for ((kind, attributesCounts) in attributesByKind) {
                printMap(p, kind.name, attributesCounts.associate { (k, v) -> k.readableString to v })
            }
        }
    }

    private fun printTaskOverview(statisticsData: Collection<CompileStatisticsData<B, P>>) {
        var allTasksTimeMs = 0L
        var kotlinTotalTimeMs = 0L
        val kotlinTasks = ArrayList<CompileStatisticsData<B, P>>()

        for (task in statisticsData) {
            val taskTimeMs = task.getDurationMs()
            allTasksTimeMs += taskTimeMs

            if (task.getFromKotlinPlugin() == true) {
                kotlinTotalTimeMs += taskTimeMs
                kotlinTasks.add(task)
            }
        }

        if (kotlinTasks.isEmpty()) {
            p.println("No Kotlin task was run")
            return
        }

        val ktTaskPercent = (kotlinTotalTimeMs.toDouble() / allTasksTimeMs * 100).asString(1)
        p.println("Total time for Kotlin tasks: ${formatTime(kotlinTotalTimeMs)} ($ktTaskPercent % of all tasks time)")

        val table = TextTable("Time", "% of Kotlin time", "Task")
        for (task in kotlinTasks.sortedWith(compareBy({ -it.getDurationMs() }, { it.getStartTimeMs() }))) {
            val timeMs = task.getDurationMs()
            val percent = (timeMs.toDouble() / kotlinTotalTimeMs * 100).asString(1)
            table.addRow(formatTime(timeMs), "$percent %", task.getTaskName())
        }
        table.printTo(p)
        p.println()
    }

    private fun printTasksLog(
        statisticsData: List<CompileStatisticsData<B, P>>,
    ) {
        for (task in statisticsData.sortedWith(compareBy({ -it.getDurationMs() }, { it.getStartTimeMs() }))) {
            printTaskLog(task)
            p.println()
        }
    }

    private fun printTaskLog(
        statisticsData: CompileStatisticsData<B, P>,
    ) {
        val skipMessage = statisticsData.getSkipMessage()
        if (skipMessage != null) {
            p.println("Task '${statisticsData.getTaskName()}' was skipped: $skipMessage")
        } else {
            p.println("Task '${statisticsData.getTaskName()}' finished in ${formatTime(statisticsData.getDurationMs())}")
        }

        statisticsData.getKotlinLanguageVersion()?.also {
            p.withIndent("Task info:") {
                p.println("Kotlin language version: $it")
            }
        }

        if (statisticsData.getIcLogLines().isNotEmpty()) {
            p.withIndent("Compilation log for task '${statisticsData.getTaskName()}':") {
                statisticsData.getIcLogLines().forEach { p.println(it) }
            }
        }

        if (printMetrics) {
            printMetrics(
                statisticsData.getBuildTimesMetrics(), statisticsData.getPerformanceMetrics(), statisticsData.getNonIncrementalAttributes(),
                statisticsData.getGcTimeMetrics(), statisticsData.getGcCountMetrics()
            )
            printCustomTaskMetrics(statisticsData)
        }
    }
}