/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

interface BuildMetricsReporter {
    fun startMeasure(time: BuildTime, startNs: Long)
    fun endMeasure(time: BuildTime, endNs: Long)
    fun addMetric(metric: BuildTime, value: Long)

    fun addAttribute(attribute: BuildAttribute)

    fun getMetrics(): BuildMetrics
    fun addMetrics(metrics: BuildMetrics?)
}

inline fun <T> BuildMetricsReporter.measure(time: BuildTime, fn: () -> T): T {
    val start = System.nanoTime()
    startMeasure(time, start)

    try {
        return fn()
    } finally {
        val end = System.nanoTime()
        endMeasure(time, end)
    }
}