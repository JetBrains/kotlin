/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class BuildTimes<T : BuildTime> : Serializable {
    private val buildTimesNs = HashMap<T, Long>()

    fun addAll(other: BuildTimes<T>) {
        for ((buildTime, timeNs) in other.buildTimesNs) {
            addTimeNs(buildTime, timeNs)
        }
    }

    fun addTimeNs(buildTime: T, timeNs: Long) {
        buildTimesNs[buildTime] = buildTimesNs.getOrDefault(buildTime, 0) + timeNs
    }

    fun addTimeMs(buildTime: T, timeMs: Long) = addTimeNs(buildTime, timeMs * 1_000_000)

    fun asMapMs(): Map<T, Long> = buildTimesNs.mapValues { it.value / 1_000_000 }

    companion object {
        const val serialVersionUID = 0L
    }
}