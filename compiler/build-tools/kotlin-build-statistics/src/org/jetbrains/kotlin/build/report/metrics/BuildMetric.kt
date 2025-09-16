/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import kotlin.reflect.KClass

sealed class BuildMetric<T : BuildMetric<T>>(open val parent: T?, val readableString: String, val name: String) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is BuildMetric<*>) {
            return this.name == other.name
        }
        return false
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

fun <T : BuildMetric<T>> getAllMetricsByType(buildMetricClass: KClass<out T>): List<T> = buildMetricClass.sealedSubclasses.flatMap {
    if (it.sealedSubclasses.isEmpty())
        listOfNotNull(it.objectInstance)
    else getAllMetricsByType(it)
}

fun getAllMetrics() = allBuildTimeMetrics + allBuildPerformanceMetrics

val allBuildTimeMetrics
    get() = getAllMetricsByType(BuildTimeMetric::class) + getAllCustomBuildTimeMetrics()

val allBuildPerformanceMetrics = getAllMetricsByType(BuildPerformanceMetric::class)
val allBuildTimeMetricsByParentMap
    get() = allBuildTimeMetrics.groupBy { it.parent }