/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.util.concurrent.CopyOnWriteArrayList

class KlibSizeMetric private constructor(path: String, parent: KlibSizeMetric?) :
    BuildPerformanceMetric(path, path, ValueType.BYTES, parent) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KlibSizeMetric) return false
        return this.name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    companion object {
        fun createIfDoesNotExistAndReturn(path: String): KlibSizeMetric {
            val parent = if (path.contains("/")) createIfDoesNotExistAndReturn(path.substringBeforeLast("/")) else null
            val newKlibSizeMetric = KlibSizeMetric(path, parent)
            allKlibSizeMetrics.addIfAbsent(newKlibSizeMetric)
            return getAllKLibSizeMetrics().find { it.name == path }
                ?: error("Cannot find metric $path") //I am not sure how it could be
        }

        const val ROOT_METRIC_NAME = "KLIB directory cumulative size"
    }

}

private val allKlibSizeMetrics = CopyOnWriteArrayList<KlibSizeMetric>()

fun getAllKLibSizeMetrics() = allKlibSizeMetrics


