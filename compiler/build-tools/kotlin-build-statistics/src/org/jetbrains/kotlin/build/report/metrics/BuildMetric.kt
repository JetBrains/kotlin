/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable
import kotlin.reflect.KClass

sealed class BuildMetric<T : BuildMetric<T>>(open val parent: T?, val readableString: String, val name: String) : Serializable {
    /**
     * Internal collection of child metrics.
     *
     * Thread-safety: This collection is NOT thread-safe. Access and modifications
     * (such as adding children) must be coordinated externally if used from multiple
     * threads. Consequently, APIs that mutate this collection (e.g., createChild in
     * BuildPerformanceMetric and BuildTimeMetric) are also not thread-safe.
     */
    protected val children = mutableListOf<T>()

    /**
     * Returns the current list of children. The returned list reflects the underlying
     * mutable collection and thus shares the same thread-safety limitations.
     */
    fun children(): List<T> = children

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

fun <T : BuildMetric<T>> getAllMetricsByType(buildTimeClass: KClass<T>): List<T> =
    buildTimeClass.sealedSubclasses.mapNotNull { it.objectInstance }.flatMap { it.children() + it }

fun getAllMetrics() = BuildMetric::class.sealedSubclasses.mapNotNull { it.objectInstance }.flatMap { it.children() + it }

val allBuildTimeMetrics
    get() = getAllMetricsByType(BuildTimeMetric::class)

val allBuildPerformanceMetrics = getAllMetricsByType(BuildPerformanceMetric::class)
val allBuildTimeMetricsMap = allBuildTimeMetrics.groupBy { it.parent }