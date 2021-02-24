/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import java.util.*

class BuildTimes : Serializable {
    private val myBuildTimes = EnumMap<BuildTime, Long>(BuildTime::class.java)

    fun addAll(other: BuildTimes) {
        for ((bt, timeNs) in other.myBuildTimes) {
            add(bt, timeNs)
        }
    }

    fun add(buildTime: BuildTime, timeNs: Long) {
        myBuildTimes[buildTime] = myBuildTimes.getOrDefault(buildTime, 0) + timeNs
    }

    fun asMap(): Map<BuildTime, Long> = myBuildTimes

    companion object {
        const val serialVersionUID = 0L
    }
}