/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

data class BuildMetrics(
    val buildTimes: BuildTimes = BuildTimes(),
    val buildPerformanceMetrics: BuildPerformanceMetrics = BuildPerformanceMetrics(),
    val buildAttributes: BuildAttributes = BuildAttributes(),
    val gcMetrics: GcMetrics = GcMetrics()
) : Serializable {
    fun addAll(other: BuildMetrics) {
        buildTimes.addAll(other.buildTimes)
        buildPerformanceMetrics.addAll(other.buildPerformanceMetrics)
        buildAttributes.addAll(other.buildAttributes)
        gcMetrics.addAll(other.gcMetrics)
    }

    companion object {
        const val serialVersionUID = 0L
    }
}