/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

data class DynamicBuildTimeEntry(val parent: BuildTime, val time: Long) : Serializable

class BuildTimes<T : BuildTime> : Serializable {
    private val buildTimesNs = HashMap<T, Long>()
    private val dynamicBuildTimesNs = HashMap<String, DynamicBuildTimeEntry>()

    fun addAll(other: BuildTimes<T>) {
        for ((buildTime, timeNs) in other.buildTimesNs) {
            addTimeNs(buildTime, timeNs)
        }

        for ((dynamicBuildTime, dynamicBuildTimeEntry) in other.dynamicBuildTimesNs) {
            addDynamicTimeNs(dynamicBuildTime, dynamicBuildTimeEntry)
        }
    }

    fun addTimeNs(buildTime: T, timeNs: Long) {
        buildTimesNs[buildTime] = buildTimesNs.getOrDefault(buildTime, 0) + timeNs
    }

    fun addDynamicTimeNs(dynamicBuildTime: String, dynamicBuildTimeEntry: DynamicBuildTimeEntry) {
        dynamicBuildTimesNs[dynamicBuildTime]?.let { (parent, timeNs) ->
            check(parent == dynamicBuildTimeEntry.parent)
            dynamicBuildTimesNs[dynamicBuildTime] = DynamicBuildTimeEntry(parent, dynamicBuildTimeEntry.time + timeNs)
        } ?: dynamicBuildTimesNs.put(dynamicBuildTime, dynamicBuildTimeEntry)
    }

    fun addTimeMs(buildTime: T, timeMs: Long) = addTimeNs(buildTime, timeMs * 1_000_000)

    fun buildTimesMapMs(): Map<T, Long> = buildTimesNs.mapValues { it.value / 1_000_000 }

    fun dynamicBuildTimesMapMs(): Map<String, DynamicBuildTimeEntry> = dynamicBuildTimesNs.mapValues {
        it.value.copy(time = it.value.time / 1_000_000)
    }

    companion object {
        const val serialVersionUID = 1L
    }
}