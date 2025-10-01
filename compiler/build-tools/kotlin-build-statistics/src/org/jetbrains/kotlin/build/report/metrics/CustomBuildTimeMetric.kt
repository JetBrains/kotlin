/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.util.concurrent.CopyOnWriteArrayList


class CustomBuildTimeMetric private constructor(name: String, parent: BuildTimeMetric?) : BuildTimeMetric(name, name, parent) {
    companion object {
        fun createIfDoesNotExistAndReturn(name: String, parent: BuildTimeMetric? = null): BuildTimeMetric {
            val newCustomBuildTimeMetric = CustomBuildTimeMetric(name, parent)
            allCustomBuildTimeMetrics.addIfAbsent(newCustomBuildTimeMetric)
            return getAllCustomBuildTimeMetrics().find { it.name == name }?: error("Cannot find metric $name") //I am not sure how it could be
        }
    }
}

private val allCustomBuildTimeMetrics = CopyOnWriteArrayList<CustomBuildTimeMetric>()

fun getAllCustomBuildTimeMetrics() = allCustomBuildTimeMetrics


