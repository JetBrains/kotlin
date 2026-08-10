/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.util.concurrent.CopyOnWriteArrayList


class CustomBuildTimeMetric private constructor(name: String, parent: BuildTimeMetric?) : BuildTimeMetric(name, name, parent) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomBuildTimeMetric) return false
        // The same lowering may run both on the first (pre-serialization) and on the second compilation stage,
        // producing dynamic phases with equal names but different parent metrics (e.g. IR_PRE_LOWERING vs IR_LOWERING).
        // Distinguish such metrics by the parent as well, otherwise their measurements get merged and the
        // performance report becomes incorrect (KT-87438).
        return this.name == other.name && this.parent == other.parent
    }

    override fun hashCode(): Int = 31 * name.hashCode() + (parent?.hashCode() ?: 0)

    companion object {
        fun createIfDoesNotExistAndReturn(name: String, parent: BuildTimeMetric? = null): BuildTimeMetric {
            val newCustomBuildTimeMetric = CustomBuildTimeMetric(name, parent)
            allCustomBuildTimeMetrics.addIfAbsent(newCustomBuildTimeMetric)
            return getAllCustomBuildTimeMetrics().find { it == newCustomBuildTimeMetric }
                ?: error("Cannot find metric $name") //I am not sure how it could be
        }
    }

}

private val allCustomBuildTimeMetrics = CopyOnWriteArrayList<CustomBuildTimeMetric>()

fun getAllCustomBuildTimeMetrics() = allCustomBuildTimeMetrics


