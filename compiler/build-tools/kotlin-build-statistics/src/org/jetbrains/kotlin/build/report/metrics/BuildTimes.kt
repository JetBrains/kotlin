/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

data class DynamicBuildTimeKey(val name: String, val parent: BuildTimeMetric) : Serializable

class BuildTimes<T : BuildTimeMetric> : Serializable {
    private val buildTimesNs = HashMap<T, Long>()
    private val dynamicBuildTimesNs = LinkedHashMap<DynamicBuildTimeKey, Long>()

    fun addAll(other: BuildTimes<T>) {
        for ((buildTime, timeNs) in other.buildTimesNs) {
            addTimeNs(buildTime, timeNs)
        }

        for ((dynamicBuildTimeKey, timeNs) in other.dynamicBuildTimesNs) {
            addDynamicTimeNs(dynamicBuildTimeKey, timeNs)
        }
    }

    fun addTimeNs(buildTime: T, timeNs: Long) {
        buildTimesNs[buildTime] = buildTimesNs.getOrDefault(buildTime, 0) + timeNs
    }

    fun addDynamicTimeNs(dynamicBuildTimeKey: DynamicBuildTimeKey, timeNs: Long) {
        dynamicBuildTimesNs[dynamicBuildTimeKey] = dynamicBuildTimesNs.getOrDefault(dynamicBuildTimeKey, 0) + timeNs
    }

    fun addTimeMs(buildTime: T, timeMs: Long) = addTimeNs(buildTime, timeMs * 1_000_000)

    fun buildTimesMapMs(): Map<T, Long> = buildTimesNs.mapValues { it.value / 1_000_000 }

    fun dynamicBuildTimesMapMs(): Map<DynamicBuildTimeKey, Long> = dynamicBuildTimesNs.mapValues { it.value / 1_000_000 }

    companion object {
        const val serialVersionUID = 1L
    }
}