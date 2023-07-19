/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class BuildPerformanceMetrics<T: BuildPerformanceMetric> : Serializable {
    companion object {
        const val serialVersionUID = 0L
    }

    private val myBuildMetrics = HashMap<T, Long>()

    fun addAll(other: BuildPerformanceMetrics<T>) {
        for ((bt, timeNs) in other.myBuildMetrics) {
            add(bt, timeNs)
        }
    }

    fun add(metric: T, value: Long = 1) {
        myBuildMetrics[metric] = myBuildMetrics.getOrDefault(metric, 0) + value
    }

    fun asMap(): Map<T, Long> = myBuildMetrics

}