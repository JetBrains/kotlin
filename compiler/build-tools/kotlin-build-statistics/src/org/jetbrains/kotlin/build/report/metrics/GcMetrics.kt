/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import kotlin.collections.HashMap

class GcMetrics : Serializable {

    private val myGcMetrics = HashMap<String, GcMetric>()
    fun addAll(gcMetrics: GcMetrics) {
        gcMetrics.myGcMetrics.forEach { (key, value) ->
            val gcMetric = myGcMetrics[key]
            myGcMetrics[key] = gcMetric?.let { gcMetric + value } ?: value
        }
    }

    fun add(metric: String, value: GcMetric) {
        myGcMetrics[metric] = value
    }

    fun asGcCountMap(): Map<String, Long> = myGcMetrics.mapValues { it.value.count }
    fun asGcTimeMap(): Map<String, Long> = myGcMetrics.mapValues { it.value.time }
    fun asMap(): Map<String, GcMetric> = myGcMetrics

    fun isEmpty() = myGcMetrics.isEmpty()
}
data class GcMetric(
    val time: Long,
    val count: Long
): Serializable {
    operator fun minus(increment: GcMetric): GcMetric {
        return GcMetric(time - increment.time, count - increment.count)
    }

    operator fun plus(increment: GcMetric?): GcMetric {
        return GcMetric(time + (increment?.time ?: 0), count + (increment?.count ?: 0))
    }
}
