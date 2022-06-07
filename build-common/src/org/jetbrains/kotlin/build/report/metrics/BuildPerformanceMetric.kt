/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

@Suppress("Reformat")
enum class BuildPerformanceMetric(val parent: BuildPerformanceMetric? = null, val readableString: String, val type: SizeMetricType) : Serializable {
    CACHE_DIRECTORY_SIZE(readableString = "Total size of the cache directory", type = SizeMetricType.BYTES),
        LOOKUP_SIZE(CACHE_DIRECTORY_SIZE, "Lookups size", type = SizeMetricType.BYTES),
        SNAPSHOT_SIZE(CACHE_DIRECTORY_SIZE, "ABI snapshot size", type = SizeMetricType.BYTES),

    BUNDLE_SIZE(readableString = "Total size of the final bundle", type = SizeMetricType.BYTES),

    COMPILE_ITERATION(parent = null, "Total compiler iteration", type = SizeMetricType.NUMBER),

    // Metrics for the `kotlin.incremental.useClasspathSnapshot` feature
    CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT(parent = null, "Number of times 'ClasspathEntrySnapshotTransform' ran", type = SizeMetricType.NUMBER),
        JAR_CLASSPATH_ENTRY_SIZE(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, "Size of jar classpath entry", type = SizeMetricType.BYTES),
        JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, "Size of jar classpath entry's snapshot", type = SizeMetricType.BYTES),
        DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT, "Size of directory classpath entry's snapshot", type = SizeMetricType.BYTES),
    COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT(parent = null, "Number of times classpath changes are computed", type = SizeMetricType.NUMBER),
    SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT(parent = null, "Number of times classpath snapshot is shrunk and saved after compilation", type = SizeMetricType.NUMBER),
        CLASSPATH_ENTRY_COUNT(parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Number of classpath entries", type = SizeMetricType.NUMBER),
        CLASSPATH_SNAPSHOT_SIZE(parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Size of classpath snapshot", type = SizeMetricType.BYTES),
        SHRUNK_CLASSPATH_SNAPSHOT_SIZE(parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Size of shrunk classpath snapshot", type = SizeMetricType.BYTES),
    LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT(parent = null, "Number of times classpath snapshot is loaded", type = SizeMetricType.NUMBER),
        LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS(parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Number of cache hits when loading classpath entry snapshots", type = SizeMetricType.NUMBER),
        LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES(parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, "Number of cache misses when loading classpath entry snapshots", type = SizeMetricType.NUMBER),
    ;

    companion object {
        const val serialVersionUID = 0L

        val children by lazy {
            values().filter { it.parent != null }.groupBy { it.parent }
        }
    }
}

enum class SizeMetricType {
    BYTES,
    NUMBER
}
