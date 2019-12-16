/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.CompilationResultCategory
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.incremental.ICReporterBase
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

internal class BuildReportICReporter(
    private val compilationResults: CompilationResults,
    rootDir: File,
    private val isVerbose: Boolean = false,
    // todo: default value
    // todo: sync BuildReportICReporterAsync
    private val reportMetrics: Boolean = true
) : ICReporterBase(rootDir), RemoteICReporter {
    private val icLogLines = arrayListOf<String>()
    private val recompilationReason = HashMap<File, String>()
    private val rootMetric = Metric("<root>", 0)
    private val metrics = ArrayDeque<Metric>().apply { add(rootMetric) }

    override fun report(message: () -> String) {
        icLogLines.add(message())
    }

    override fun reportVerbose(message: () -> String) {
        if (isVerbose) {
            report(message)
        }
    }

    override fun startMeasure(metric: String, startNs: Long) {
        if (!reportMetrics) return

        val newMetric = Metric(metric, startNs)
        if (metrics.isNotEmpty()) {
            metrics.peekLast().children.add(newMetric)
        }
        metrics.addLast(newMetric)
    }

    override fun endMeasure(metric: String, endNs: Long) {
        if (!reportMetrics) return

        while (metrics.isNotEmpty()) {
            val lastMetric = metrics.peekLast()
            if (lastMetric.name == metric) {
                lastMetric.endNs = endNs
                break
            } else {
                metrics.removeLast()
            }
        }
    }

    override fun reportCompileIteration(incremental: Boolean, sourceFiles: Collection<File>, exitCode: ExitCode) {
        if (!incremental) return

        icLogLines.add("Compile iteration:")
        for (file in sourceFiles) {
            val reason = recompilationReason[file]?.let { " <- $it" } ?: ""
            icLogLines.add("  ${file.relativeOrCanonical()}$reason")
        }
        recompilationReason.clear()
    }

    override fun reportMarkDirty(affectedFiles: Iterable<File>, reason: String) {
        affectedFiles.forEach { recompilationReason[it] = reason }
    }

    override fun flush() {
        if (reportMetrics) {
            icLogLines.add("Performance metrics:")
            reportMetric(rootMetric)
        }

        compilationResults.add(CompilationResultCategory.BUILD_REPORT_LINES.code, icLogLines)
    }

    private fun reportMetric(metric: Metric, level: Int = 0) {
        if (level > 0) {
            val timeMs = metric.endNs?.let { (it - metric.startNs) / 1_000_000L }
            icLogLines.add("  ".repeat(level) + "{perf_metric:${metric.name}} ${timeMs ?: "<unknown>"} ms")
        }

        metric.children.forEach { reportMetric(it, level + 1) }
    }
}

private class Metric(val name: String, val startNs: Long) {
    var endNs: Long? = null
    val children = ArrayList<Metric>()
}