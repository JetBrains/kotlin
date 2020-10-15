/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.jetbrains.kotlin.idea.perf.Stats
import org.jetbrains.kotlin.idea.perf.calcMean
import java.util.ArrayList

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Benchmark(
    @set:JsonProperty("agentName")
    var agentName: String?,
    @set:JsonProperty("benchmark")
    var benchmark: String,
    @set:JsonProperty("name")
    var name: String? = null,
    @set:JsonProperty("synthetic")
    var synthetic: Boolean? = null,
    @set:JsonProperty("buildTimestamp")
    var buildTimestamp: String,
    @set:JsonProperty("buildBranch")
    var buildBranch: String?,
    @set:JsonProperty("buildId")
    var buildId: Int?,
    @set:JsonProperty("metricValue")
    var metricValue: Long? = null,
    @set:JsonProperty("metricError")
    var metricError: Long? = null,
    @set:JsonProperty("hasError")
    var hasError: Boolean? = null,

    @set:JsonProperty("metrics")
    var metrics: List<Metric> = ArrayList()
) {
    init {
        hasError = if (metrics.any { it.ifHasError() == true }) true else null
    }

    fun resetValue() {
        buildId = null
        metricValue = null
        metricError = null
        buildBranch = null
        metrics.forEach { it.resetValue() }
    }

    fun fileName(): String =
        listOfNotNull(benchmark, name?.replace("[^A-Za-z0-9_]", ""), buildId?.toString()).joinToString(separator = "_")

    fun cleanUp() {
        metrics?.forEach { it.cleanUp() }
        metrics = metrics?.filter { it.metricValue != null || (it.metrics?.isNotEmpty() == true) }
    }

    fun merge(extraBenchmark: Benchmark) {
        val benchmarkMetrics = extraBenchmark.metrics.filter {
            it.metricName !in Stats.extraMetricNames
        }
        benchmarkMetrics.forEach { it.rawMetrics = null }
        val extraBenchmarkName = extraBenchmark.name!!
        metrics = metrics.filterNot { it.metricName == extraBenchmarkName } + Metric(
            metricName = extraBenchmarkName,
            metricValue = extraBenchmark.metricValue,
            metricError = extraBenchmark.metricError,
            hasError = extraBenchmark.hasError,
            metrics = benchmarkMetrics
        )
        cleanUp()

        hasError = if (metrics.any { it.ifHasError() == true }) true else null
        val values = metrics.mapNotNull { it.metricValue }.toLongArray()
        val calcMean = calcMean(values)
        metricValue = calcMean.geomMean.toLong()
        metricError = calcMean.stdDev.toLong()
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Metric(
    @set:JsonProperty("metricName")
    var metricName: String,
    @set:JsonProperty("legacyName")
    var legacyName: String? = null,
    @set:JsonProperty("metricValue")
    var metricValue: Long?,
    @set:JsonProperty("metricError")
    var metricError: Long? = null,
    @set:JsonProperty("hasError")
    var hasError: Boolean? = null,
    @set:JsonProperty("metrics")
    var metrics: List<Metric>? = null,
    @set:JsonProperty("rawMetrics")
    var rawMetrics: List<Metric>? = null
) {
    fun ifHasError(): Boolean? {
        if (hasError == true) return true
        hasError =
            if ((metrics?.any { it.ifHasError() == true } == true) || (rawMetrics?.any { it.ifHasError() == true } == true)) {
                true
            } else {
                null
            }
        return hasError
    }

    fun resetValue() {
        metricValue = null
        metricError = null
        metrics?.forEach { it.resetValue() }
        rawMetrics?.forEach { it.resetValue() }
    }

    fun cleanUp() {
        legacyName = null
        metrics?.forEach { it.cleanUp() }
        rawMetrics?.forEach { it.cleanUp() }

        metrics = metrics?.filter { it.metricValue != null || (it.metrics?.isNotEmpty() == true) }
        if (metrics?.isEmpty() == true) metrics = null

        rawMetrics = rawMetrics?.filter { it.metricValue != null || (it.metrics?.isNotEmpty() == true) }
        if (rawMetrics?.isEmpty() == true) rawMetrics = null
    }
}
