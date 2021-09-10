/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable


@Suppress("Reformat")
enum class BuildPerformanceMetric(val parent: BuildPerformanceMetric? = null, val readableString: String, val type: BuildMetricType) : Serializable {
    CACHE_DIRECTORY_SIZE(readableString = "Total size of the cache directory", type = BuildMetricType.FILE_SIZE),
        LOOKUP_SIZE(CACHE_DIRECTORY_SIZE, "Lookups size", type = BuildMetricType.FILE_SIZE),
        SNAPSHOT_SIZE(CACHE_DIRECTORY_SIZE, "ABI snapshot size", type = BuildMetricType.FILE_SIZE),

    COMPILE_ITERATION(parent = null, "Total compiler iteration", type = BuildMetricType.NUMBER),

    // Metrics for the `kotlin.incremental.useClasspathSnapshot` feature
    ORIGINAL_CLASSPATH_SNAPSHOT_SIZE(parent = null, "Size of the original classpath snapshot (before shrinking)", type = BuildMetricType.FILE_SIZE),
    SHRUNK_CLASSPATH_SNAPSHOT_SIZE(parent = null, "Size of the shrunk classpath snapshot", type = BuildMetricType.FILE_SIZE),
    ;

    companion object {
        const val serialVersionUID = 0L

        val children by lazy {
            values().filter { it.parent != null }.groupBy { it.parent }
        }
    }
}

enum class BuildMetricType {
    TIME_IN_MS,
    FILE_SIZE,
    NUMBER
}