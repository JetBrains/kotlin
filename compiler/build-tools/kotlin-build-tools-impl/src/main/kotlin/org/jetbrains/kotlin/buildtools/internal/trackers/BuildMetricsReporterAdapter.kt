/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.trackers

import org.jetbrains.kotlin.build.report.metrics.*
import org.jetbrains.kotlin.buildtools.api.trackers.BuildMetricsCollector
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl.Companion.METRICS_COLLECTOR

internal class BuildMetricsReporterAdapter(private val collector: BuildMetricsCollector) :
    BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric> {
    private val myBuildTimeStartNs = HashMap<BuildTimeMetric, Long>()
    private val myGcPerformance = HashMap<String, GcMetric>()

    override fun startMeasure(time: BuildTimeMetric) {
        if (time in myBuildTimeStartNs) {
            error("$time was restarted before it finished")
        }
        myBuildTimeStartNs[time] = System.nanoTime()
    }

    override fun endMeasure(time: BuildTimeMetric) {
        val startNs = myBuildTimeStartNs.remove(time) ?: error("$time finished before it started")
        val durationNs = System.nanoTime() - startNs
        collector.collectMetric(time.readableString, BuildMetricsCollector.ValueType.NANOSECONDS, durationNs)
    }

    override fun addTimeMetricNs(time: BuildTimeMetric, durationNs: Long) {
        collector.collectMetric(time.readableString, BuildMetricsCollector.ValueType.NANOSECONDS, durationNs)
    }

    override fun addMetric(metric: BuildPerformanceMetric, value: Long) {
        collector.collectMetric(metric.readableString, metric.type.toMetricsReporterType(), value)
    }

    override fun addTimeMetric(metric: BuildPerformanceMetric) {
        val time = when (metric.type) {
            ValueType.NANOSECONDS -> System.nanoTime()
            ValueType.MILLISECONDS, ValueType.TIME -> System.currentTimeMillis()
            else -> error("Unable to add time metric for '${metric.type}' type")
        }
        collector.collectMetric(metric.readableString, metric.type.toMetricsReporterType(), time)
    }

    override fun addGcMetric(metric: String, value: GcMetric) {
        collector.collectMetric(metric, BuildMetricsCollector.ValueType.MILLISECONDS, value.time)
    }

    override fun startGcMetric(name: String, value: GcMetric) {
        if (name in myGcPerformance) {
            error("$name was restarted before it finished")
        }
        myGcPerformance[name] = value
    }

    override fun endGcMetric(name: String, value: GcMetric) {
        val startValue = myGcPerformance.remove(name) ?: error("$name finished before it started")
        val diff = value - startValue
        collector.collectMetric(name, BuildMetricsCollector.ValueType.MILLISECONDS, diff.time)
    }

    override fun addAttribute(attribute: BuildAttribute) {
        collector.collectMetric(attribute.readableString, BuildMetricsCollector.ValueType.ATTRIBUTE, 1)
    }

    override fun getMetrics(): BuildMetrics<BuildTimeMetric, BuildPerformanceMetric> {
        error("Not supported")
    }

    override fun addMetrics(metrics: BuildMetrics<out BuildTimeMetric, out BuildPerformanceMetric>) {
        metrics.buildAttributes.asMap().forEach { (attribute, value) ->
            repeat(value) { addAttribute(attribute) }
        }
        metrics.buildTimes.buildTimesMapMs().forEach { (time, value) ->
            addTimeMetricNs(time, value * 1_000_000)
        }
        metrics.buildPerformanceMetrics.asMap().forEach { (metric, value) ->
            addMetric(metric, value)
        }
        metrics.gcMetrics.asMap().forEach { (metric, value) ->
            addGcMetric(metric, value)
        }
    }
}

private fun ValueType.toMetricsReporterType(): BuildMetricsCollector.ValueType {
    return when (this) {
        ValueType.NANOSECONDS -> BuildMetricsCollector.ValueType.NANOSECONDS
        ValueType.BYTES -> BuildMetricsCollector.ValueType.BYTES
        ValueType.MILLISECONDS -> BuildMetricsCollector.ValueType.MILLISECONDS
        ValueType.NUMBER -> BuildMetricsCollector.ValueType.NUMBER
        ValueType.TIME -> BuildMetricsCollector.ValueType.TIME
    }
}

internal fun BuildOperationImpl<*>.getMetricsReporter(): BuildMetricsReporter<BuildTimeMetric, BuildPerformanceMetric> =
    this[METRICS_COLLECTOR]?.let { BuildMetricsReporterAdapter(it) } ?: if (this[BuildOperationImpl.XX_KGP_METRICS_COLLECTOR]) {
        BuildMetricsReporterImpl()
    } else {
        DoNothingBuildMetricsReporter
    }
