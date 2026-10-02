/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*

class BuildPerformanceMetrics<T : BuildPerformanceMetric> : Serializable {
    companion object {
        const val serialVersionUID = 0L
    }

    private val myBuildMetrics = HashMap<T, Long>()

    fun addAll(other: BuildPerformanceMetrics<out T>) {
        for ([bt, value] in other.myBuildMetrics) {
            addLong(bt, value)
        }
    }

    fun addLong(metric: T, value: Long = 1) {
        val current = myBuildMetrics[metric]
        myBuildMetrics[metric] = when {
            current == null -> value
            // Timestamps must not be summed: the earliest start of the measurement is kept
            metric is GradleBuildTimeMetricStart -> minOf(current, value)
            else -> current + value
        }
    }

    fun asMap(): Map<T, Long> = myBuildMetrics

}
