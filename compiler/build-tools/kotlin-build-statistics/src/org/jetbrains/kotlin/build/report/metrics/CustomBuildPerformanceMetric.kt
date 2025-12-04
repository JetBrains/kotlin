/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.util.concurrent.CopyOnWriteArrayList

class CustomBuildPerformanceMetric private constructor(name: String, parent: BuildPerformanceMetric?, type: ValueType) :
    BuildPerformanceMetric(name, name, type, parent) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomBuildPerformanceMetric) return false
        return this.name == other.name
    }

    override fun hashCode(): Int = name.hashCode()

    companion object {
        fun createIfDoesNotExistAndReturn(name: String, type: ValueType, parent: BuildPerformanceMetric? = null): BuildPerformanceMetric {
            val newCustomBuildPerformanceMetric = CustomBuildPerformanceMetric(name, parent, type)
            allCustomBuildPerformanceMetrics.addIfAbsent(newCustomBuildPerformanceMetric)
            return getAllCustomBuildPerformanceMetrics().find { it.name == name }
                ?: error("Cannot find metric $name") //I am not sure how it could be
        }
    }

}

private val allCustomBuildPerformanceMetrics = CopyOnWriteArrayList<CustomBuildPerformanceMetric>()

fun getAllCustomBuildPerformanceMetrics() = allCustomBuildPerformanceMetrics


