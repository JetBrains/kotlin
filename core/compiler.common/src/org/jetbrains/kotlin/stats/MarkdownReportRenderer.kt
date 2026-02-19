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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MarkdownReportRenderer(val statsCalculator: StatsCalculator) {
    companion object {
        const val TOP_MODULE_COUNT = 10
    }

    val unitStats = statsCalculator.unitStats
    val totalStats = statsCalculator.totalStats
    val isTimestampMode = statsCalculator.reportsData is TimestampReportsData
    val aggregatedStats = (if (isTimestampMode) statsCalculator.averageStats else totalStats)

    fun render(): String {
        return buildString {
            renderInfo()
            renderAggregateTimeStats()
            renderKlibStats()
            renderSystemStats()
            if (statsCalculator.unitStats.size > 1) {
                renderTopUnitStats()
            }
        }
    }

    private fun StringBuilder.renderInfo() {
        appendLine("# Stats for ${totalStats.getTitleName(total = true)}")
        appendLine()
        appendLine("* Platform: ${totalStats.platform}")
        appendLine("* Has errors: ${totalStats.hasErrors}")
        appendLine("* Modules count: ${unitStats.size}")
        appendLine("* Files count: ${totalStats.filesCount}")
        appendLine("* Lines count: ${totalStats.linesCount}")
        appendLine()
    }

    private fun StringBuilder.renderAggregateTimeStats() {
        val aggregatedTime = aggregatedStats.getTotalTime()
        appendLine("# ${if (isTimestampMode) "Average" else "Total"} time")
        appendLine()

        val maxTotalTime = maxOf(aggregatedTime.nanos, aggregatedTime.userNanos, aggregatedTime.cpuNanos)
        val nameMaxColumnWidth = maxOf(phaseTypeName.values.maxOf { it.length }, phaseSideTypeName.values.maxOf { it.length })
        val valueMaxColumnWidth = formatTime(1.0, maxTotalTime).length

        with(MarkdownTableRenderer(4, nameMaxColumnWidth, valueMaxColumnWidth)) {
            fun renderTimeLine(name: String, time: Time?) {
                if (time == null || time == Time.ZERO) return
                val timeRatio = time / aggregatedTime

                renderLine(
                    name,
                    formatTime(timeRatio.nanos, time.nanos),
                    formatTime(timeRatio.userNanos, time.userNanos),
                    formatTime(timeRatio.cpuNanos, time.cpuNanos)
                )
            }

            renderHeader("Phase", "Absolute", "User", "Cpu")

            aggregatedStats.forEachPhaseMeasurement { phaseType, time ->
                renderTimeLine(phaseTypeName.getValue(phaseType), time)
                aggregatedStats.dynamicStats?.forEach { (parentPhaseType, name, time) ->
                    if (parentPhaseType == phaseType) renderTimeLine("↳ $name", time)
                }
            }

            renderBreak()

            aggregatedStats.forEachPhaseSideMeasurement { phaseSideType, time ->
                renderTimeLine(phaseSideTypeName.getValue(phaseSideType), time?.time)
            }

            renderBreak()

            renderTimeLine("TOTAL", aggregatedTime)
        }

        appendLine()
    }

    private fun StringBuilder.renderSystemStats() {
        appendLine("# System stats" + if (isTimestampMode) " (Average)" else "")
        appendLine()
        appendLine("* JIT time: ${aggregatedStats.jitTimeMillis} ms")
        appendLine("* GC stats:")
        for (gcStats in aggregatedStats.gcStats) {
            appendLine("  * ${gcStats.kind}: ${gcStats.millis} ms (${gcStats.count} collections)")
        }
        appendLine()
    }

    private fun StringBuilder.renderKlibStats() {
        totalStats.klibElementStats?.let { stats ->
            appendLine("# KLIB stats")
            stats.forEach { (path, size) ->
                appendLine("* KLIB element '$path' has size of $size Bytes")
            }
        }
    }

    private fun StringBuilder.renderTopUnitStats() {
        appendLine("# Slowest ${if (isTimestampMode) "runs" else "modules"}")
        appendLine()

        renderTopModules(
            "total time",
            max = true,
            selector = { it.getTotalTime().nanos },
            printer = { moduleResult, totalResult -> formatTime(moduleResult.toDouble() / totalResult, moduleResult) }
        )
        renderTopModules(
            "analysis time",
            max = true,
            selector = { it.analysisStats?.nanos ?: 0 },
            printer = { moduleResult, totalResult -> formatTime(moduleResult.toDouble() / totalResult, moduleResult) }
        )

        renderTopModules(
            "LPS (lines per second)",
            max = false,
            selector = { (it.linesCount.toDouble() / it.getTotalTime().nanos) * nanosInSecond },
            printer = { moduleResult, _ -> String.format("%.2f LPS", moduleResult) }
        )

        // Other metrics can be easily added in the same way
    }

    private fun <R : Comparable<R>> StringBuilder.renderTopModules(
        name: String,
        max: Boolean,
        selector: (UnitStats) -> R,
        printer: (moduleResult: R, totalResult: R) -> String,
    ) {
        appendLine("## By $name")
        appendLine()

        val totalValue = selector(totalStats)
        val topAnalysisModules = statsCalculator.getTopModulesBy(TOP_MODULE_COUNT, max, selector = selector)
        val firstColumnName = if (statsCalculator.reportsData is TimestampReportsData) "Time Stamp" else "Module"
        val nameMaxColumnWidth = maxOf(firstColumnName.length, topAnalysisModules.maxOf { it.getTitleName(total = false).length })
        val valueMaxColumnWidth = printer(selector(if (max) topAnalysisModules.first() else topAnalysisModules.last()), totalValue).length

        with(MarkdownTableRenderer(2, nameMaxColumnWidth, valueMaxColumnWidth)) {
            renderHeader(firstColumnName, "Value")
            for (module in topAnalysisModules) {
                renderLine(module.getTitleName(total = false), printer(selector(module), totalValue))
            }
        }

        appendLine()
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
            prefix + dateTimeFormatter.format(timeStampMs)
        }
    }

    private val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("UTC")
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

    fun StringBuilder.renderHeader(vararg columns: String) {
        require(columns.size == columnsCount) { "Columns count must be $columnsCount" }

        renderLine(*columns)

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

        renderLine(*tableFormattingColumns)
    }

    fun StringBuilder.renderBreak() {
        renderLine(*emptyColumns)
    }

    fun StringBuilder.renderLine(vararg columns: String) {
        require(columns.size == columnsCount) { "Columns count must be $columnsCount" }

        appendLine(String.format(columnsFormat, *columns))
    }
}