/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.lang.management.ManagementFactory

interface BuildMetricsReporter<B : BuildTime, P : BuildPerformanceMetric> {
    fun startMeasure(time: B)
    fun endMeasure(time: B)
    fun addTimeMetricNs(time: B, durationNs: Long)
    fun addTimeMetricMs(time: B, durationMs: Long) = addTimeMetricNs(time, durationMs * 1_000_000)

    fun addMetric(metric: P, value: Long)
    fun addTimeMetric(metric: P)

    //Change metric to enum if possible
    fun addGcMetric(metric: String, value: GcMetric)
    fun startGcMetric(name: String, value: GcMetric)
    fun endGcMetric(name: String, value: GcMetric)

    fun addAttribute(attribute: BuildAttribute)

    fun getMetrics(): BuildMetrics<B, P>
    fun addMetrics(metrics: BuildMetrics<B, P>)
}

inline fun <B : BuildTime, P : BuildPerformanceMetric, T> BuildMetricsReporter<B, P>.measure(time: B, fn: () -> T): T {
    startMeasure(time)
    try {
        return fn()
    } finally {
        endMeasure(time)
    }
}


fun <B : BuildTime, P : BuildPerformanceMetric> BuildMetricsReporter<B, P>.startMeasureGc() {
    ManagementFactory.getGarbageCollectorMXBeans().forEach {
        startGcMetric(it.name, GcMetric(it.collectionTime, it.collectionCount))
    }
}

fun <B : BuildTime, P : BuildPerformanceMetric> BuildMetricsReporter<B, P>.endMeasureGc() {
    ManagementFactory.getGarbageCollectorMXBeans().forEach {
        endGcMetric(it.name, GcMetric(it.collectionTime, it.collectionCount))
    }
}