/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

interface BuildMetricsReporter {
    fun startMeasure(metric: BuildTime, startNs: Long)
    fun endMeasure(metric: BuildTime, endNs: Long)

    fun addAttribute(attribute: BuildAttribute)

    fun getMetrics(): BuildMetrics
    fun addMetrics(metrics: BuildMetrics?)
}

inline fun <T> BuildMetricsReporter.measure(metric: BuildTime, fn: () -> T): T {
    val start = System.nanoTime()
    startMeasure(metric, start)

    try {
        return fn()
    } finally {
        val end = System.nanoTime()
        endMeasure(metric, end)
    }
}