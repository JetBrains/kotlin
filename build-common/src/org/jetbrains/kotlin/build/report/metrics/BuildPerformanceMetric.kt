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
    CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT(parent = null, "Number of times 'ClasspathEntrySnapshotTransform' ran", type = BuildMetricType.NUMBER),
        CLASSPATH_ENTRY_SIZE(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, "Size of classpath entry (directory or jar)", type = BuildMetricType.FILE_SIZE),
        CLASSPATH_ENTRY_SNAPSHOT_SIZE(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, "Size of classpath entry's snapshot", type = BuildMetricType.FILE_SIZE),
    SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT(parent = null, "Number of times classpath snapshot is shrunk and saved after compilation", type = BuildMetricType.NUMBER),
        CLASSPATH_ENTRY_COUNT(parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Number of classpath entries", type = BuildMetricType.NUMBER),
        CLASSPATH_SNAPSHOT_SIZE(parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Size of classpath snapshot", type = BuildMetricType.FILE_SIZE),
        SHRUNK_CLASSPATH_SNAPSHOT_SIZE(parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Size of shrunk classpath snapshot", type = BuildMetricType.FILE_SIZE),
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