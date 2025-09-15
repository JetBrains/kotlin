/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.util.concurrent.CopyOnWriteArrayList


class CustomBuildTimeMetric private constructor(parent: BuildTimeMetric?, name: String) : BuildTimeMetric(parent, name, name) {
    companion object {
        fun createIfDoesNotExistAndReturn(parent: BuildTimeMetric? = null, name: String): BuildTimeMetric {
            val newCustomBuildTimeMetric = CustomBuildTimeMetric(parent, name)
            allCustomBuildTimeMetrics.addIfAbsent(newCustomBuildTimeMetric)
            return getAllCustomBuildTimeMetrics().find { it.name == name } ?: newCustomBuildTimeMetric
        }
    }
}

private val allCustomBuildTimeMetrics = CopyOnWriteArrayList<CustomBuildTimeMetric>()

fun getAllCustomBuildTimeMetrics() = allCustomBuildTimeMetrics


