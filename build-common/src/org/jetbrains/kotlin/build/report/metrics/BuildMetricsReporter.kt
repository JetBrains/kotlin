/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

interface BuildMetricsReporter {
    fun startMeasure(time: BuildTime)
    fun endMeasure(time: BuildTime)
    fun addTimeMetricNs(time: BuildTime, durationNs: Long)
    fun addTimeMetricMs(time: BuildTime, durationMs: Long) = addTimeMetricNs(time, durationMs * 1_000_000)

    fun addMetric(metric: BuildPerformanceMetric, value: Long)

    fun addAttribute(attribute: BuildAttribute)

    fun getMetrics(): BuildMetrics
    fun addMetrics(metrics: BuildMetrics)
}

inline fun <T> BuildMetricsReporter.measure(time: BuildTime, fn: () -> T): T {
    startMeasure(time)
    try {
        return fn()
    } finally {
        endMeasure(time)
    }
}