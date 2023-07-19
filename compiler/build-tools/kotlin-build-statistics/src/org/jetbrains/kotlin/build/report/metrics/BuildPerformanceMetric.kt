/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

interface BuildPerformanceMetric : BuildTime, Serializable {
    override fun getParent(): BuildPerformanceMetric?

    override fun getAllMetrics(): List<BuildPerformanceMetric>

    override fun children(): List<BuildPerformanceMetric>?

    fun getType(): ValueType
}

enum class JpsBuildPerformanceMetric(
    private val parent: JpsBuildPerformanceMetric? = null,
    private val readableString: String,
    private val type: ValueType,
) : BuildPerformanceMetric {
    DAEMON_GC_TIME(readableString = "Time spent in GC", type = ValueType.NANOSECONDS),
    DAEMON_GC_COUNT(readableString = "Count of GC", type = ValueType.NUMBER),

    COMPILE_ITERATION(parent = null, "Total compiler iteration", type = ValueType.NUMBER),
    ANALYZED_LINES_NUMBER(parent = COMPILE_ITERATION, "Number of lines analyzed", type = ValueType.NUMBER),
    CODE_GENERATED_LINES_NUMBER(parent = COMPILE_ITERATION, "Number of lines for code generation", type = ValueType.NUMBER),
    ANALYSIS_LPS(parent = COMPILE_ITERATION, "Analysis lines per second", type = ValueType.NUMBER),
    CODE_GENERATION_LPS(parent = COMPILE_ITERATION, "Code generation lines per second", type = ValueType.NUMBER),
    ;

    override fun getReadableString(): String = readableString
    override fun getType(): ValueType = type

    override fun getParent(): BuildPerformanceMetric? = parent

    override fun children(): List<BuildPerformanceMetric>? {
        return children[this]
    }

    override fun getName(): String = this.name

    override fun getAllMetrics(): List<BuildPerformanceMetric> {
        return entries
    }

    companion object {
        const val serialVersionUID = 1L

        val children by lazy {
            entries.filter { it.parent != null }.groupBy { it.parent }
        }
    }
}

@Suppress("Reformat")
enum class GradleBuildPerformanceMetric(
    private val parent: GradleBuildPerformanceMetric? = null,
    private val readableString: String,
    private val type: ValueType,
) :
    BuildPerformanceMetric {
    CACHE_DIRECTORY_SIZE(readableString = "Total size of the cache directory", type = ValueType.BYTES),
    LOOKUP_SIZE(CACHE_DIRECTORY_SIZE, "Lookups size", type = ValueType.BYTES),
    SNAPSHOT_SIZE(CACHE_DIRECTORY_SIZE, "ABI snapshot size", type = ValueType.BYTES),

    BUNDLE_SIZE(readableString = "Total size of the final bundle", type = ValueType.BYTES),

    DAEMON_INCREASED_MEMORY(readableString = "Increase memory usage", type = ValueType.BYTES),
    DAEMON_MEMORY_USAGE(readableString = "Total memory usage at the end of build", type = ValueType.BYTES),

    DAEMON_GC_TIME(readableString = "Time spent in GC", type = ValueType.NANOSECONDS),
    DAEMON_GC_COUNT(readableString = "Count of GC", type = ValueType.NUMBER),

    COMPILE_ITERATION(parent = null, "Total compiler iteration", type = ValueType.NUMBER),
    ANALYZED_LINES_NUMBER(parent = COMPILE_ITERATION, "Number of lines analyzed", type = ValueType.NUMBER),
    CODE_GENERATED_LINES_NUMBER(parent = COMPILE_ITERATION, "Number of lines for code generation", type = ValueType.NUMBER),
    ANALYSIS_LPS(parent = COMPILE_ITERATION, "Analysis lines per second", type = ValueType.NUMBER),
    CODE_GENERATION_LPS(parent = COMPILE_ITERATION, "Code generation lines per second", type = ValueType.NUMBER),

    // Metrics for the `kotlin.incremental.useClasspathSnapshot` feature
    CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT(
        parent = null,
        "Number of times 'ClasspathEntrySnapshotTransform' ran",
        type = ValueType.NUMBER
    ),
    JAR_CLASSPATH_ENTRY_SIZE(
        parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
        "Size of jar classpath entry",
        type = ValueType.BYTES
    ),
    JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE(
        parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
        "Size of jar classpath entry's snapshot",
        type = ValueType.BYTES
    ),
    DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE(
        parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
        "Size of directory classpath entry's snapshot",
        type = ValueType.BYTES
    ),
    COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT(parent = null, "Number of times classpath changes are computed", type = ValueType.NUMBER),
    SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT(
        parent = null,
        "Number of times classpath snapshot is shrunk and saved after compilation",
        type = ValueType.NUMBER
    ),
    CLASSPATH_ENTRY_COUNT(
        parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
        "Number of classpath entries",
        type = ValueType.NUMBER
    ),
    CLASSPATH_SNAPSHOT_SIZE(
        parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
        "Size of classpath snapshot",
        type = ValueType.BYTES
    ),
    SHRUNK_CLASSPATH_SNAPSHOT_SIZE(
        parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
        "Size of shrunk classpath snapshot",
        type = ValueType.BYTES
    ),
    LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT(parent = null, "Number of times classpath snapshot is loaded", type = ValueType.NUMBER),
    LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS(
        parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
        "Number of cache hits when loading classpath entry snapshots",
        type = ValueType.NUMBER
    ),
    LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES(
        parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
        "Number of cache misses when loading classpath entry snapshots",
        type = ValueType.NUMBER
    ),

    //time metrics
    START_TASK_ACTION_EXECUTION(readableString = "Start time of task action", type = ValueType.TIME),
    FINISH_KOTLIN_DAEMON_EXECUTION(readableString = "Finish time of kotlin daemon execution", type = ValueType.TIME),

    CALL_KOTLIN_DAEMON(readableString = "Finish gradle part of task execution", type = ValueType.NANOSECONDS),
    CALL_WORKER(readableString = "Worker submit time", type = ValueType.NANOSECONDS),
    START_WORKER_EXECUTION(readableString = "Start time of worker execution", type = ValueType.NANOSECONDS),
    START_KOTLIN_DAEMON_EXECUTION(readableString = "Start time of kotlin daemon task execution", type = ValueType.NANOSECONDS),
    ;

    override fun getReadableString(): String = readableString

    override fun getParent(): BuildPerformanceMetric? = parent
    override fun getType(): ValueType = type

    override fun getName(): String = this.name

    override fun children(): List<BuildPerformanceMetric>? {
        return children[this]
    }

    override fun getAllMetrics(): List<BuildPerformanceMetric> {
        return entries
    }

    companion object {
        const val serialVersionUID = 1L

        val children by lazy {
            entries.filter { it.parent != null }.groupBy { it.parent }
        }
    }
}

enum class ValueType {
    BYTES,
    NUMBER,
    NANOSECONDS,
    MILLISECONDS,
    TIME
}
