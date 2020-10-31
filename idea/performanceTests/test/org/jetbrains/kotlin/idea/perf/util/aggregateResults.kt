/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import java.io.File
import java.util.*

fun main(args: Array<String>) {
    val argFile = File(args[0])
    val groupBy = argFile.listFiles()
        .filter { it.length() > 0 && it.name.startsWith("stats-") && it.extension == "json" }
        .map { it.loadBenchmark() }
        .groupBy { it.benchmark }

    groupBy.forEach { (k, benchmarks) ->
        if (benchmarks.isEmpty()) return@forEach

        benchmarks.forEach { benchmark ->
            benchmark.metrics.firstOrNull { it.metricName == "_value" }?.let { metric ->
                metric.rawMetrics?.firstOrNull { it.warmUp == true && it.index == 0 }?.let {
                    val warmUpBenchmark = Benchmark(
                        agentName = benchmark.agentName,
                        buildBranch = benchmark.buildBranch,
                        buildId = benchmark.buildId,
                        benchmark = benchmark.benchmark,
                        name = benchmark.name,
                        warmUp = it.warmUp,
                        index = it.index,
                        hasError = it.hasError,
                        buildTimestamp = benchmark.buildTimestamp,
                        metrics = it.metrics ?: emptyList()
                    )
                    warmUpBenchmark.writeJson()
                }
            }
        }

        // build geom mean benchmark
        val first = benchmarks.first()
        val geomMeanBenchmark = Benchmark(
            agentName = first.agentName,
            buildBranch = first.buildBranch,
            buildId = first.buildId,
            benchmark = first.benchmark,
            synthetic = true,
            name = "geomMean",
            buildTimestamp = first.buildTimestamp
        )

        benchmarks
            .filter { it.synthetic != true && it.warmUp != true }
            .forEach { geomMeanBenchmark.merge(it) }
        geomMeanBenchmark.writeJson()
    }
}