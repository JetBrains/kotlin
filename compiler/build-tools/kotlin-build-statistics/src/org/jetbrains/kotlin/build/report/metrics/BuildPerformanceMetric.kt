/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

sealed class BuildPerformanceMetric private constructor(
    parent: BuildPerformanceMetric?,
    readableString: String,
    val type: ValueType,
    name: String,
) : BuildTime<BuildPerformanceMetric>(parent, readableString, name), Serializable {

    constructor(readableString: String, type: ValueType, name: String) : this(null, readableString, type, name)

    /**
     * Creates a child performance metric and registers it in this metric's children.
     *
     * Thread-safety: Not thread-safe. This method mutates the underlying children list
     * of BuildTime which is not a thread-safe collection. If accessed from multiple
     * threads, external synchronization is required.
     */
    fun createChild(readableString: String, type: ValueType, name: String): BuildPerformanceMetric {
        return ChildPerformanceMetric(this, readableString, type, name).also { children.add(it) }
    }

    internal class ChildPerformanceMetric internal constructor(
        parent: BuildPerformanceMetric,
        readableString: String,
        type: ValueType,
        name: String,
    ) : BuildPerformanceMetric(parent, readableString, type, name)
}


object DAEMON_GC_TIME : BuildPerformanceMetric(
    readableString = "Time spent in GC",
    type = ValueType.NANOSECONDS,
    name = "DAEMON_GC_TIME"
)

object DAEMON_GC_COUNT : BuildPerformanceMetric(
    readableString = "Count of GC",
    type = ValueType.NUMBER,
    name = "DAEMON_GC_COUNT"
)

object COMPILE_ITERATION : BuildPerformanceMetric(
    readableString = "Total compiler iteration",
    type = ValueType.NUMBER,
    name = "COMPILE_ITERATION"
)

val IC_COMPILE_ITERATION = COMPILE_ITERATION.createChild(
    readableString = "Total kotlin compiler iteration",
    type = ValueType.NUMBER,
    name = "IC_COMPILE_ITERATION"
)

val SOURCE_LINES_NUMBER = COMPILE_ITERATION.createChild(
    readableString = "Number of lines analyzed",
    type = ValueType.NUMBER,
    name = "SOURCE_LINES_NUMBER"
)

val ANALYSIS_LPS = COMPILE_ITERATION.createChild(
    readableString = "Analysis lines per second",
    type = ValueType.NUMBER,
    name = "ANALYSIS_LPS"
)

val CODE_GENERATION_LPS = COMPILE_ITERATION.createChild(
    readableString = "Code generation lines per second",
    type = ValueType.NUMBER,
    name = "CODE_GENERATION_LPS"
)

val jpsBuildPerformanceValues = listOf(
    DAEMON_GC_TIME,
    DAEMON_GC_COUNT,
    COMPILE_ITERATION,
    IC_COMPILE_ITERATION,
    SOURCE_LINES_NUMBER,
    ANALYSIS_LPS,
    CODE_GENERATION_LPS
)

object CACHE_DIRECTORY_SIZE : BuildPerformanceMetric(
    readableString = "Total size of the cache directory",
    type = ValueType.BYTES,
    name = "CACHE_DIRECTORY_SIZE"
)

val LOOKUP_SIZE = CACHE_DIRECTORY_SIZE.createChild(
    readableString = "Lookups size",
    type = ValueType.BYTES,
    name = "LOOKUP_SIZE"
)

val SNAPSHOT_SIZE = CACHE_DIRECTORY_SIZE.createChild(
    readableString = "ABI snapshot size",
    type = ValueType.BYTES,
    name = "SNAPSHOT_SIZE"
)

object BUNDLE_SIZE : BuildPerformanceMetric(
    readableString = "Total size of the final bundle",
    type = ValueType.BYTES,
    name = "BUNDLE_SIZE"
)

object DAEMON_INCREASED_MEMORY : BuildPerformanceMetric(
    readableString = "Increase memory usage",
    type = ValueType.BYTES,
    name = "DAEMON_INCREASED_MEMORY"
)

object DAEMON_MEMORY_USAGE : BuildPerformanceMetric(
    readableString = "Total memory usage at the end of build",
    type = ValueType.BYTES,
    name = "DAEMON_MEMORY_USAGE"
)

val TRANSLATION_TO_IR_LPS = COMPILE_ITERATION.createChild(
    readableString = "Translation to IR lines per second",
    type = ValueType.NUMBER,
    name = "TRANSLATION_TO_IR_LPS"
)

val IR_PRE_LOWERING_LPS = COMPILE_ITERATION.createChild(
    readableString = "IR pre-lowering lines per second",
    type = ValueType.NUMBER,
    name = "IR_PRE_LOWERING_LPS"
)

val IR_SERIALIZATION_LPS = COMPILE_ITERATION.createChild(
    readableString = "IR serialization lines per second",
    type = ValueType.NUMBER,
    name = "IR_SERIALIZATION_LPS"
)

val KLIB_WRITING_LPS = COMPILE_ITERATION.createChild(
    readableString = "KLib Writing lines per second",
    type = ValueType.NUMBER,
    name = "KLIB_WRITING_LPS"
)

val IR_LOWERING_LPS = COMPILE_ITERATION.createChild(
    readableString = "IR Lowering lines per second",
    type = ValueType.NUMBER,
    name = "IR_LOWERING_LPS"
)

val BACKEND_LPS = COMPILE_ITERATION.createChild(
    readableString = "Backend lines per second",
    type = ValueType.NUMBER,
    name = "BACKEND_LPS"
)

// Metrics for the `kotlin.incremental.useClasspathSnapshot` feature
object CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT : BuildPerformanceMetric(
    readableString = "Number of times 'ClasspathEntrySnapshotTransform' ran",
    type = ValueType.NUMBER,
    name = "CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT"
)

val JAR_CLASSPATH_ENTRY_SIZE = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT.createChild(
    readableString = "Size of jar classpath entry",
    type = ValueType.BYTES,
    name = "JAR_CLASSPATH_ENTRY_SIZE"
)

val JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT.createChild(
    readableString = "Size of jar classpath entry's snapshot",
    type = ValueType.BYTES,
    name = "JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE"
)

val DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT.createChild(
    readableString = "Size of directory classpath entry's snapshot",
    type = ValueType.BYTES,
    name = "DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE"
)

object COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT : BuildPerformanceMetric(
    readableString = "Number of times classpath changes are computed",
    type = ValueType.NUMBER,
    name = "COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT"
)

object SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT : BuildPerformanceMetric(
    readableString = "Number of times classpath snapshot is shrunk and saved after compilation",
    type = ValueType.NUMBER,
    name = "SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT"
)

val CLASSPATH_ENTRY_COUNT = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT.createChild(
    readableString = "Number of classpath entries",
    type = ValueType.NUMBER,
    name = "CLASSPATH_ENTRY_COUNT"
)

val CLASSPATH_SNAPSHOT_SIZE = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT.createChild(
    readableString = "Size of classpath snapshot",
    type = ValueType.BYTES,
    name = "CLASSPATH_SNAPSHOT_SIZE"
)

val SHRUNK_CLASSPATH_SNAPSHOT_SIZE = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT.createChild(
    readableString = "Size of shrunk classpath snapshot",
    type = ValueType.BYTES,
    name = "SHRUNK_CLASSPATH_SNAPSHOT_SIZE"
)

object LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT : BuildPerformanceMetric(
    readableString = "Number of times classpath snapshot is loaded",
    type = ValueType.NUMBER,
    name = "LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT"
)

val LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT.createChild(
    readableString = "Number of cache hits when loading classpath entry snapshots",
    type = ValueType.NUMBER,
    name = "LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS"
)

val LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT.createChild(
    readableString = "Number of cache misses when loading classpath entry snapshots",
    type = ValueType.NUMBER,
    name = "LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES"
)

//time metrics
object START_TASK_ACTION_EXECUTION : BuildPerformanceMetric(
    readableString = "Start time of task action",
    type = ValueType.TIME,
    name = "START_TASK_ACTION_EXECUTION"
)

object FINISH_KOTLIN_DAEMON_EXECUTION : BuildPerformanceMetric(
    readableString = "Finish time of kotlin daemon execution",
    type = ValueType.TIME,
    name = "FINISH_KOTLIN_DAEMON_EXECUTION"
)

object CALL_KOTLIN_DAEMON : BuildPerformanceMetric(
    readableString = "Finish gradle part of task execution",
    type = ValueType.NANOSECONDS,
    name = "CALL_KOTLIN_DAEMON"
)

object CALL_WORKER : BuildPerformanceMetric(
    readableString = "Worker submit time",
    type = ValueType.NANOSECONDS,
    name = "CALL_WORKER"
)

object START_WORKER_EXECUTION : BuildPerformanceMetric(
    readableString = "Start time of worker execution",
    type = ValueType.NANOSECONDS,
    name = "START_WORKER_EXECUTION"
)

object START_KOTLIN_DAEMON_EXECUTION : BuildPerformanceMetric(
    readableString = "Start time of kotlin daemon task execution",
    type = ValueType.NANOSECONDS,
    name = "START_KOTLIN_DAEMON_EXECUTION"
)

val gradlePerformanceMetrics = listOf(
    CACHE_DIRECTORY_SIZE,
    LOOKUP_SIZE,
    SNAPSHOT_SIZE,
    BUNDLE_SIZE,
    DAEMON_INCREASED_MEMORY,
    DAEMON_MEMORY_USAGE,
    DAEMON_GC_TIME,
    DAEMON_GC_COUNT,
    COMPILE_ITERATION,
    SOURCE_LINES_NUMBER,
    ANALYSIS_LPS,
    CODE_GENERATION_LPS,
    TRANSLATION_TO_IR_LPS,
    IR_PRE_LOWERING_LPS,
    IR_SERIALIZATION_LPS,
    KLIB_WRITING_LPS,
    IR_LOWERING_LPS,
    BACKEND_LPS,
    CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
    JAR_CLASSPATH_ENTRY_SIZE,
    JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE,
    DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE,
    COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT,
    SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    CLASSPATH_ENTRY_COUNT,
    CLASSPATH_SNAPSHOT_SIZE,
    SHRUNK_CLASSPATH_SNAPSHOT_SIZE,
    LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS,
    LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES,
    START_TASK_ACTION_EXECUTION,
    FINISH_KOTLIN_DAEMON_EXECUTION,
    CALL_KOTLIN_DAEMON,
    CALL_WORKER,
    START_WORKER_EXECUTION,
    START_KOTLIN_DAEMON_EXECUTION
)

sealed class ValueType {
    object BYTES : ValueType()
    object NUMBER : ValueType()
    object NANOSECONDS : ValueType()
    object MILLISECONDS : ValueType()
    object TIME : ValueType()

    companion object {
        val values = listOf(BYTES, NUMBER, NANOSECONDS, MILLISECONDS, TIME)
    }
}
