/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import org.jetbrains.kotlin.util.forEachPhaseMeasurement
import org.jetbrains.kotlin.util.forEachPhaseSideMeasurement
import org.jetbrains.kotlin.util.nanosInSecond
import org.jetbrains.kotlin.util.phaseSideTypeName
import org.jetbrains.kotlin.util.phaseTypeName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class MarkdownReportRenderer(val statsCalculator: StatsCalculator) {
    companion object {
        const val TOP_MODULE_COUNT = 10
    }

    val unitStats = statsCalculator.unitStats
    val totalStats = statsCalculator.totalStats

    fun render() {
        printInfo()
        printTotalStats()
        printSystemStats()
        if (statsCalculator.unitStats.size > 1) {
            printTopUnitStats()
        }
    }

    private fun printInfo() {
        println("# Stats for ${totalStats.getTitleName(total = true)}")
        println()
        println("* Platform: ${totalStats.platform}")
        println("* Has errors: ${totalStats.hasErrors}")
        println("* Modules count: ${unitStats.size}")
        println("* Files count: ${totalStats.filesCount}")
        println("* Lines count: ${totalStats.linesCount}")
        println()
    }

    private fun printTotalStats() {
        val totalTime = totalStats.getTotalTime()
        println("# Total time")
        println()

        val maxTotalTime = maxOf(totalTime.nanos, totalTime.userNanos, totalTime.cpuNanos)
        val nameMaxColumnWidth = maxOf(phaseTypeName.values.maxOf { it.length }, phaseSideTypeName.values.maxOf { it.length })
        val valueMaxColumnWidth = formatTime(1.0, maxTotalTime).length

        with(MarkdownTableRenderer(4, nameMaxColumnWidth, valueMaxColumnWidth)) {
            fun printTimeLine(name: String, time: Time?, wholeTime: Time) {
                if (time == null || time == Time.ZERO) return
                val timeRatio = time / wholeTime

                printLine(
                    name,
                    formatTime(timeRatio.nanos, time.nanos),
                    formatTime(timeRatio.userNanos, time.userNanos),
                    formatTime(timeRatio.cpuNanos, time.cpuNanos)
                )
            }

            printHeader("Phase", "Absolute", "User", "Cpu")

            totalStats.forEachPhaseMeasurement { phaseType, time ->
                printTimeLine(phaseTypeName.getValue(phaseType), time, totalTime)
            }

            printBreak()

            totalStats.forEachPhaseSideMeasurement { phaseSideType, time ->
                printTimeLine(phaseSideTypeName.getValue(phaseSideType), time?.time, totalTime)
            }

            printBreak()

            printTimeLine("TOTAL", totalTime, totalTime)
        }

        println()
    }

    private fun printSystemStats() {
        println("# System stats")
        println()
        println("* JIT time: ${totalStats.jitTimeMillis} ms")
        println("* GC stats:")
        for (gcStats in totalStats.gcStats) {
            println("  * ${gcStats.kind}: ${gcStats.millis} ms (${gcStats.count} collections)")
        }
        println()
    }

    private fun printTopUnitStats() {
        println("# Slowest ${if (statsCalculator.reportsData is TimestampReportsData) "runs" else "modules"}")
        println()

        printTopModules(
            "total time",
            max = true,
            selector = { it.getTotalTime().nanos },
            printer = { moduleResult, totalResult -> formatTime(moduleResult.toDouble() / totalResult, moduleResult) }
        )
        printTopModules(
            "analysis time",
            max = true,
            selector = { it.analysisStats?.nanos ?: 0 },
            printer = { moduleResult, totalResult -> formatTime(moduleResult.toDouble() / totalResult, moduleResult) }
        )

        printTopModules(
            "LPS (lines per second)",
            max = false,
            selector = { (it.linesCount.toDouble() / it.getTotalTime().nanos) * nanosInSecond },
            printer = { moduleResult, _ -> String.format("%.2f LPS", moduleResult) }
        )

        // Other metrics can be easily added in the same way
    }

    private fun <R : Comparable<R>> printTopModules(
        name: String,
        max: Boolean,
        selector: (UnitStats) -> R,
        printer: (moduleResult: R, totalResult: R) -> String,
    ) {
        println("## By $name")
        println()

        val totalValue = selector(totalStats)
        val topAnalysisModules = statsCalculator.getTopModulesBy(TOP_MODULE_COUNT, max, selector = selector)
        val firstColumnName = if (statsCalculator.reportsData is TimestampReportsData) "Time Stamp" else "Module"
        val nameMaxColumnWidth = maxOf(firstColumnName.length, topAnalysisModules.maxOf { it.getTitleName(total = false).length })
        val valueMaxColumnWidth = printer(selector(if (max) topAnalysisModules.first() else topAnalysisModules.last()), totalValue).length

        with(MarkdownTableRenderer(2, nameMaxColumnWidth, valueMaxColumnWidth)) {
            printHeader(firstColumnName, "Value")
            for (module in topAnalysisModules) {
                printLine(module.getTitleName(total = false), printer(selector(module), totalValue))
            }
        }

        println()
    }

    private fun formatTime(ratio: Double, nanos: Long): String {
        return String.format("%.2f%% (%d ms)", ratio * 100, TimeUnit.NANOSECONDS.toMillis(nanos))
    }

    private fun UnitStats.getTitleName(total: Boolean): String {
        return if (statsCalculator.reportsData is TimestampReportsData && total ||
            statsCalculator.reportsData is ModulesReportsData && !total
        ) {
            name ?: ""
        } else {
            val prefix = if (statsCalculator.unitStats.size == 1) "$name at " else ""
            prefix + dateTimeWithoutMsFormatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStampMs), ZoneId.systemDefault()))
        }
    }

    private val dateTimeWithoutMsFormatter by lazy(LazyThreadSafetyMode.PUBLICATION) {
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }
}

class MarkdownTableRenderer(val columnsCount: Int, val nameColumnWidth: Int, val valueColumnWidth: Int) {
    private val columnsFormat =
        buildString {
            for (i in 0 until columnsCount) {
                val formatString = if (i == 0) "| %-${nameColumnWidth + 1}s|" else "%${valueColumnWidth + 1}s |"
                append(formatString)
            }
        }

    private val emptyColumns by lazy { Array(columnsCount) { "" } }

    fun printHeader(vararg columns: String) {
        require(columns.size == columnsCount) { "Columns count must be $columnsCount" }

        printLine(*columns)

        val tableFormattingColumns = buildList {
            val valueColumnTableFormat = "-".repeat(valueColumnWidth - 1) + ':'
            for (i in 0 until columns.size) {
                add(
                    if (i == 0)
                        "-".repeat(nameColumnWidth)
                    else
                        valueColumnTableFormat
                )
            }
        }.toTypedArray()

        printLine(*tableFormattingColumns)
    }

    fun printBreak() {
        printLine(*emptyColumns)
    }

    fun printLine(vararg columns: String) {
        require(columns.size == columnsCount) { "Columns count must be $columnsCount" }

        println(String.format(columnsFormat, *columns))
    }
}