/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import kotlin.collections.plus
import kotlin.reflect.KClass

private fun <T : BuildPerformanceMetric> getAllMetricsByType(buildMetricClass: KClass<out T>): List<T> =
    buildMetricClass.sealedSubclasses.flatMap {
        if (it.sealedSubclasses.isEmpty())
            listOfNotNull(it.objectInstance)
        else getAllMetricsByType(it)
    }

fun getAllMetrics() = allBuildPerformanceMetrics + getAllCustomBuildTimeMetrics()

val allBuildTimeMetrics: List<BuildTimeMetric>
    get() = getAllMetricsByType(BuildTimeMetric::class) + getAllCustomBuildTimeMetrics()

internal val allBuildPerformanceMetrics
    get() = getAllMetricsByType(BuildPerformanceMetric::class) + getAllCustomBuildPerformanceMetrics()

val allBuildTimeMetricsByParentMap
    get() = allBuildTimeMetrics.groupBy { it.parent }