/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.build.report.statistics.file

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.build.report.statistics.*

internal fun Printer.printBuildReport(
    data: ReadableFileReportData,
    printMetrics: Boolean,
    printCustomTaskMetrics: Printer.(CompileStatisticsData) -> Unit,
) {
    // NOTE: BuildExecutionData / BuildOperationRecord contains data for both tasks and transforms.
    // Where possible, we still use the term "tasks" because saying "tasks/transforms" is a bit verbose and "build operations" may sound
    // a bit unfamiliar.

    printBuildInfo(data.startParameters, data.failureMessages)
    if (printMetrics && data.statisticsData.isNotEmpty()) {
        printMetrics(
            data.statisticsData.map { it.getBuildTimesMetrics() }.reduce { agg, value ->
                (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
            },
            data.statisticsData.map { it.getDynamicBuildTimeMetrics() }.reduce { agg, value ->
                (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
            },
            data.statisticsData.map { it.getPerformanceMetrics() }.reduce { agg, value ->
                (agg.keys + value.keys).associateWith { (agg[it] ?: 0) + (value[it] ?: 0) }
            },
            data.statisticsData.map { it.getNonIncrementalAttributes().asSequence() }.reduce { agg, value -> agg + value }.toList(),
            aggregatedMetric = true,
        )
        println()
    }
    printTaskOverview(data.statisticsData)
    printTasksLog(data.statisticsData, printMetrics, printCustomTaskMetrics)
}

private fun Printer.printBuildInfo(startParameters: BuildStartParameters, failureMessages: List<String>) {
    withIndent("Gradle start parameters:") {
        startParameters.let {
            println("tasks = ${it.tasks}")
            println("excluded tasks = ${it.excludedTasks}")
            println("current dir = ${it.currentDir}")
            println("project properties args = ${it.projectProperties}")
            println("system properties args = ${it.systemProperties}")
        }
    }
    println()

    if (failureMessages.isNotEmpty()) {
        println("Build failed: ${failureMessages}")
        println()
    }
}

private fun Printer.printMetrics(
    buildTimesMetrics: Map<out BuildTimeMetric, Long>,
    dynamicBuildTimesMetrics: Map<DynamicBuildTimeKey, Long>,
    performanceMetrics: Map<out BuildPerformanceMetric, Long>,
    nonIncrementalAttributes: Collection<BuildAttribute>,
    gcTimeMetrics: Map<String, Long>? = emptyMap(),
    gcCountMetrics: Map<String, Long>? = emptyMap(),
    aggregatedMetric: Boolean = false,
) {
    printBuildTimes(buildTimesMetrics, dynamicBuildTimesMetrics)
    if (aggregatedMetric) println()

    printBuildPerformanceMetrics(performanceMetrics)
    if (aggregatedMetric) println()

    printBuildAttributes(nonIncrementalAttributes)

    //TODO: KT-57310 Implement build GC metric in
    if (!aggregatedMetric) {
        printGcMetrics(gcTimeMetrics, gcCountMetrics)
    }
}

private fun Printer.printGcMetrics(
    gcTimeMetrics: Map<String, Long>?,
    gcCountMetrics: Map<String, Long>?,
) {
    val keys = HashSet<String>()
    gcCountMetrics?.keys?.also { keys.addAll(it) }
    gcTimeMetrics?.keys?.also { keys.addAll(it) }
    if (keys.isEmpty()) return

    withIndent("GC metrics:") {
        for (key in keys) {
            println("$key:")
            withIndent {
                gcCountMetrics?.get(key)?.also { println("GC count: ${it}") }
                gcTimeMetrics?.get(key)?.also { println("GC time: ${formatTime(it)}") }
            }
        }
    }
}

private fun Printer.printBuildTimes(buildTimes: Map<out BuildTimeMetric, Long>, dynamicBuildTimes: Map<DynamicBuildTimeKey, Long>) {
    if (buildTimes.isEmpty()) return

    println("Time metrics:")
    withIndent {
        val visitedBuildTimes = HashSet<BuildTimeMetric>()
        val dynamicBuildTimesMap = dynamicBuildTimes.map { (key, time) ->
            key.parent to (key.name to time)
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )

        fun printBuildTime(buildTimeMetric: BuildTimeMetric) {
            if (!visitedBuildTimes.add(buildTimeMetric)) return

            val timeMs = buildTimes[buildTimeMetric]
            if (timeMs != null) {
                println("${buildTimeMetric.readableString}: ${formatTime(timeMs)}")
                withIndent {
                    dynamicBuildTimesMap[buildTimeMetric]?.forEach { (name, timeMs) ->
                        println("$name: ${formatTime(timeMs)}")
                    }
                    buildTimeMetric.children().forEach { printBuildTime(it) }
                }
            } else {
                //Skip formatting if parent metric does not set
                buildTimeMetric.children().forEach { printBuildTime(it) }
            }
        }

        //TODO do we need to sort them?
        for (buildTime in buildTimes.keys) {
            if (buildTime.parent != null) continue

            printBuildTime(buildTime)
        }
    }
}

private fun Printer.printBuildPerformanceMetrics(buildMetrics: Map<out BuildPerformanceMetric, Long>) {
    if (buildMetrics.isEmpty()) return

    withIndent("Size metrics:") {
        for (metric in buildMetrics.keys) {
            buildMetrics[metric]?.let { printSizeMetric(metric, it) }
        }
    }
}

private fun Printer.printSizeMetric(sizeMetric: BuildPerformanceMetric, value: Long) {
    fun BuildPerformanceMetric.numberOfAncestors(): Int {
        var count = 0
        var parent: BuildPerformanceMetric? = parent
        while (parent != null) {
            count++
            parent = parent.parent
        }
        return count
    }

    val indentLevel = sizeMetric.numberOfAncestors()

    repeat(indentLevel) { pushIndent() }
    when (sizeMetric.type) {
        ValueType.BYTES -> println("${sizeMetric.readableString}: ${formatSize(value)}")
        ValueType.NUMBER -> println("${sizeMetric.readableString}: $value")
        ValueType.NANOSECONDS -> println("${sizeMetric.readableString}: $value")
        ValueType.MILLISECONDS -> println("${sizeMetric.readableString}: ${formatTime(value)}")
        ValueType.TIME -> println("${sizeMetric.readableString}: ${formatter.format(value)}")
    }
    repeat(indentLevel) { popIndent() }
}

private fun Printer.printBuildAttributes(buildAttributes: Collection<BuildAttribute>) {
    if (buildAttributes.isEmpty()) return

    val buildAttributesMap = buildAttributes.groupingBy { it }.eachCount()
    withIndent("Build attributes:") {
        val attributesByKind = buildAttributesMap.entries.groupBy { it.key.kind }.toSortedMap()
        for ((kind, attributesCounts) in attributesByKind) {
            printMap(this, kind.name, attributesCounts.associate { (k, v) -> k.readableString to v })
        }
    }
}

private fun Printer.printTaskOverview(statisticsData: Collection<CompileStatisticsData>) {
    var allTasksTimeMs = 0L
    var kotlinTotalTimeMs = 0L
    val kotlinTasks = ArrayList<CompileStatisticsData>()

    for (task in statisticsData) {
        val taskTimeMs = task.getDurationMs()
        allTasksTimeMs += taskTimeMs

        if (task.getFromKotlinPlugin() == true) {
            kotlinTotalTimeMs += taskTimeMs
            kotlinTasks.add(task)
        }
    }

    if (kotlinTasks.isEmpty()) {
        println("No Kotlin task was run")
        return
    }

    val ktTaskPercent = (kotlinTotalTimeMs.toDouble() / allTasksTimeMs * 100).asString(1)
    println("Total time for Kotlin tasks: ${formatTime(kotlinTotalTimeMs)} ($ktTaskPercent % of all tasks time)")

    val table = TextTable("Time", "% of Kotlin time", "Task")
    for (task in kotlinTasks.sortedWith(compareBy({ -it.getDurationMs() }, { it.getStartTimeMs() }))) {
        val timeMs = task.getDurationMs()
        val percent = (timeMs.toDouble() / kotlinTotalTimeMs * 100).asString(1)
        table.addRow(formatTime(timeMs), "$percent %", task.getTaskName())
    }
    table.printTo(this)
    println()
}

private fun Printer.printTasksLog(
    statisticsData: List<CompileStatisticsData>,
    printMetrics: Boolean,
    printCustomTaskMetrics: Printer.(CompileStatisticsData) -> Unit,
) {
    for (taskData in statisticsData.sortedWith(compareBy({ -it.getDurationMs() }, { it.getStartTimeMs() }))) {
        printTaskLog(taskData)
        if (printMetrics) {
            printMetrics(
                buildTimesMetrics = taskData.getBuildTimesMetrics(),
                dynamicBuildTimesMetrics = taskData.getDynamicBuildTimeMetrics(),
                performanceMetrics = taskData.getPerformanceMetrics(),
                nonIncrementalAttributes = taskData.getNonIncrementalAttributes(),
                gcTimeMetrics = taskData.getGcTimeMetrics(),
                gcCountMetrics = taskData.getGcCountMetrics()
            )
            printCustomTaskMetrics(taskData)
        }
        println()
    }
}


private fun Printer.printTaskLog(
    statisticsData: CompileStatisticsData,
) {
    val skipMessage = statisticsData.getSkipMessage()
    if (skipMessage != null) {
        println("Task '${statisticsData.getTaskName()}' was skipped: $skipMessage")
    } else {
        println("Task '${statisticsData.getTaskName()}' finished in ${formatTime(statisticsData.getDurationMs())}")
    }

    statisticsData.getKotlinLanguageVersion()?.also {
        withIndent("Task info:") {
            println("Kotlin language version: $it")
        }
    }

    if (statisticsData.getIcLogLines().isNotEmpty()) {
        withIndent("Compilation log for task '${statisticsData.getTaskName()}':") {
            statisticsData.getIcLogLines().forEach { println(it) }
        }
    }
}
