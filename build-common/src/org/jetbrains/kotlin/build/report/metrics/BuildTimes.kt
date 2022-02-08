/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*

class BuildTimes : Serializable {
    private val buildTimesNs = EnumMap<BuildTime, Long>(BuildTime::class.java)

    fun addAll(other: BuildTimes) {
        for ((buildTime, timeNs) in other.buildTimesNs) {
            addTimeNs(buildTime, timeNs)
        }
    }

    fun addTimeNs(buildTime: BuildTime, timeNs: Long) {
        buildTimesNs[buildTime] = buildTimesNs.getOrDefault(buildTime, 0) + timeNs
    }

    fun addTimeMs(buildTime: BuildTime, timeMs: Long) = addTimeNs(buildTime, timeMs * 1_000_000)

    fun asMapNs(): Map<BuildTime, Long> = buildTimesNs

    fun asMapMs(): Map<BuildTime, Long> = buildTimesNs.mapValues { it.value / 1_000_000 }

    companion object {
        const val serialVersionUID = 0L
    }
}