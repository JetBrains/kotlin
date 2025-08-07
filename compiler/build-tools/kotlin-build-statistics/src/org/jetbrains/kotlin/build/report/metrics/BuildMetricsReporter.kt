/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.lang.management.ManagementFactory

interface BuildMetricsReporter {
    fun startMeasure(time: BuildTimeMetric)
    fun endMeasure(time: BuildTimeMetric)
    fun addTimeMetricNs(time: BuildTimeMetric, durationNs: Long)
    fun addDynamicTimeMetricNs(time: String, parent: BuildTimeMetric, durationNs: Long)

    @Deprecated("Use addTimeMetricNs instead", ReplaceWith("addTimeMetricNs(time, durationNs)"))
    fun addTimeMetricMs(time: BuildTimeMetric, durationMs: Long) = addTimeMetricNs(time, durationMs * 1_000_000)

    fun addMetric(metric: BuildPerformanceMetric, value: Long)
    fun addTimeMetric(metric: BuildPerformanceMetric)

    //Change metric to enum if possible
    fun addGcMetric(metric: String, value: GcMetric)
    fun startGcMetric(name: String, value: GcMetric)
    fun endGcMetric(name: String, value: GcMetric)

    fun addAttribute(attribute: BuildAttribute)

    fun getMetrics(): BuildMetrics
    fun addMetrics(metrics: BuildMetrics)
}

inline fun <T> BuildMetricsReporter.measure(time: BuildTimeMetric, fn: () -> T): T {
    startMeasure(time)
    try {
        return fn()
    } finally {
        endMeasure(time)
    }
}


fun BuildMetricsReporter.startMeasureGc() {
    ManagementFactory.getGarbageCollectorMXBeans().forEach {
        startGcMetric(it.name, GcMetric(it.collectionTime, it.collectionCount))
    }
}

fun BuildMetricsReporter.endMeasureGc() {
    ManagementFactory.getGarbageCollectorMXBeans().forEach {
        endGcMetric(it.name, GcMetric(it.collectionTime, it.collectionCount))
    }
}