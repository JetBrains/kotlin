/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

sealed class BuildPerformanceMetric(
    parent: BuildPerformanceMetric?,
    readableString: String,
    val type: ValueType,
    name: String,
) : BuildMetric<BuildPerformanceMetric>(parent, readableString, name), Serializable {

    constructor(readableString: String, type: ValueType, name: String) : this(null, readableString, type, name)
}

sealed class JpsBuildPerformanceMetric(parent: JpsBuildPerformanceMetric? = null, readableString: String, type: ValueType, name: String) :
    BuildPerformanceMetric(parent, readableString, type, name)


object DAEMON_GC_TIME : JpsBuildPerformanceMetric(
    readableString = "Time spent in GC",
    type = ValueType.NANOSECONDS,
    name = "DAEMON_GC_TIME"
)

object DAEMON_GC_COUNT : JpsBuildPerformanceMetric(
    readableString = "Count of GC",
    type = ValueType.NUMBER,
    name = "DAEMON_GC_COUNT"
)

object JPS_COMPILE_ITERATION : JpsBuildPerformanceMetric(
    readableString = "Total compiler iteration",
    type = ValueType.NUMBER,
    name = "COMPILE_ITERATION"
)

object JPS_IC_COMPILE_ITERATION : JpsBuildPerformanceMetric(
    parent = JPS_COMPILE_ITERATION,
    readableString = "Total kotlin compiler iteration",
    type = ValueType.NUMBER,
    name = "IC_COMPILE_ITERATION"
)

object JPS_SOURCE_LINES_NUMBER : JpsBuildPerformanceMetric(
    parent = JPS_COMPILE_ITERATION,
    readableString = "Number of lines analyzed",
    type = ValueType.NUMBER,
    name = "SOURCE_LINES_NUMBER"
)

object JPS_ANALYSIS_LPS : JpsBuildPerformanceMetric(
    parent = JPS_COMPILE_ITERATION,
    readableString = "Analysis lines per second",
    type = ValueType.NUMBER,
    name = "ANALYSIS_LPS"
)

object JPS_CODE_GENERATION_LPS : JpsBuildPerformanceMetric(
    parent = JPS_COMPILE_ITERATION,
    readableString = "Code generation lines per second",
    type = ValueType.NUMBER,
    name = "CODE_GENERATION_LPS"
)

sealed class GradleBuildPerformanceMetric(parent: GradleBuildPerformanceMetric? = null, readableString: String, type: ValueType, name: String) :
    BuildPerformanceMetric(parent, readableString, type, name)

object COMPILE_ITERATION : GradleBuildPerformanceMetric(
    readableString = "Total compiler iteration",
    type = ValueType.NUMBER,
    name = "COMPILE_ITERATION"
)

object IC_COMPILE_ITERATION : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "Total kotlin compiler iteration",
    type = ValueType.NUMBER,
    name = "IC_COMPILE_ITERATION"
)

object SOURCE_LINES_NUMBER : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "Number of lines analyzed",
    type = ValueType.NUMBER,
    name = "SOURCE_LINES_NUMBER"
)

object ANALYSIS_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "Analysis lines per second",
    type = ValueType.NUMBER,
    name = "ANALYSIS_LPS"
)

object CODE_GENERATION_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "Code generation lines per second",
    type = ValueType.NUMBER,
    name = "CODE_GENERATION_LPS"
)
object CACHE_DIRECTORY_SIZE : GradleBuildPerformanceMetric(
    readableString = "Total size of the cache directory",
    type = ValueType.BYTES,
    name = "CACHE_DIRECTORY_SIZE"
)

object LOOKUP_SIZE : GradleBuildPerformanceMetric(
    parent = CACHE_DIRECTORY_SIZE,
    readableString = "Lookups size",
    type = ValueType.BYTES,
    name = "LOOKUP_SIZE"
)

object SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    parent = CACHE_DIRECTORY_SIZE,
    readableString = "ABI snapshot size",
    type = ValueType.BYTES,
    name = "SNAPSHOT_SIZE"
)

object BUNDLE_SIZE : GradleBuildPerformanceMetric(
    readableString = "Total size of the final bundle",
    type = ValueType.BYTES,
    name = "BUNDLE_SIZE"
)

object DAEMON_INCREASED_MEMORY : GradleBuildPerformanceMetric(
    readableString = "Increase memory usage",
    type = ValueType.BYTES,
    name = "DAEMON_INCREASED_MEMORY"
)

object DAEMON_MEMORY_USAGE : GradleBuildPerformanceMetric(
    readableString = "Total memory usage at the end of build",
    type = ValueType.BYTES,
    name = "DAEMON_MEMORY_USAGE"
)

object TRANSLATION_TO_IR_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "Translation to IR lines per second",
    type = ValueType.NUMBER,
    name = "TRANSLATION_TO_IR_LPS"
)

object IR_PRE_LOWERING_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "IR pre-lowering lines per second",
    type = ValueType.NUMBER,
    name = "IR_PRE_LOWERING_LPS"
)

object IR_SERIALIZATION_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "IR serialization lines per second",
    type = ValueType.NUMBER,
    name = "IR_SERIALIZATION_LPS"
)

object KLIB_WRITING_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "KLib Writing lines per second",
    type = ValueType.NUMBER,
    name = "KLIB_WRITING_LPS"
)

object IR_LOWERING_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "IR Lowering lines per second",
    type = ValueType.NUMBER,
    name = "IR_LOWERING_LPS"
)

object BACKEND_LPS : GradleBuildPerformanceMetric(
    parent = COMPILE_ITERATION,
    readableString = "Backend lines per second",
    type = ValueType.NUMBER,
    name = "BACKEND_LPS"
)

// Metrics for the `kotlin.incremental.useClasspathSnapshot` feature
object CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    readableString = "Number of times 'ClasspathEntrySnapshotTransform' ran",
    type = ValueType.NUMBER,
    name = "CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT"
)

object JAR_CLASSPATH_ENTRY_SIZE : GradleBuildPerformanceMetric(
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
    readableString = "Size of jar classpath entry",
    type = ValueType.BYTES,
    name = "JAR_CLASSPATH_ENTRY_SIZE"
)

object JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
    readableString = "Size of jar classpath entry's snapshot",
    type = ValueType.BYTES,
    name = "JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE"
)

object DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT,
    readableString = "Size of directory classpath entry's snapshot",
    type = ValueType.BYTES,
    name = "DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE"
)

object COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    readableString = "Number of times classpath changes are computed",
    type = ValueType.NUMBER,
    name = "COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT"
)

object SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    readableString = "Number of times classpath snapshot is shrunk and saved after compilation",
    type = ValueType.NUMBER,
    name = "SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT"
)

object CLASSPATH_ENTRY_COUNT : GradleBuildPerformanceMetric(
    parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    readableString = "Number of classpath entries",
    type = ValueType.NUMBER,
    name = "CLASSPATH_ENTRY_COUNT"
)

object CLASSPATH_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    readableString = "Size of classpath snapshot",
    type = ValueType.BYTES,
    name = "CLASSPATH_SNAPSHOT_SIZE"
)

object SHRUNK_CLASSPATH_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    readableString = "Size of shrunk classpath snapshot",
    type = ValueType.BYTES,
    name = "SHRUNK_CLASSPATH_SNAPSHOT_SIZE"
)

object LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    readableString = "Number of times classpath snapshot is loaded",
    type = ValueType.NUMBER,
    name = "LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT"
)

object LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS : GradleBuildPerformanceMetric(
    parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    readableString = "Number of cache hits when loading classpath entry snapshots",
    type = ValueType.NUMBER,
    name = "LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS"
)

object LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES : GradleBuildPerformanceMetric(
    parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT,
    readableString = "Number of cache misses when loading classpath entry snapshots",
    type = ValueType.NUMBER,
    name = "LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES"
)

//time metrics
object START_TASK_ACTION_EXECUTION : GradleBuildPerformanceMetric(
    readableString = "Start time of task action",
    type = ValueType.TIME,
    name = "START_TASK_ACTION_EXECUTION"
)

object FINISH_KOTLIN_DAEMON_EXECUTION : GradleBuildPerformanceMetric(
    readableString = "Finish time of kotlin daemon execution",
    type = ValueType.TIME,
    name = "FINISH_KOTLIN_DAEMON_EXECUTION"
)

object CALL_KOTLIN_DAEMON : GradleBuildPerformanceMetric(
    readableString = "Finish gradle part of task execution",
    type = ValueType.NANOSECONDS,
    name = "CALL_KOTLIN_DAEMON"
)

object CALL_WORKER : GradleBuildPerformanceMetric(
    readableString = "Worker submit time",
    type = ValueType.NANOSECONDS,
    name = "CALL_WORKER"
)

object START_WORKER_EXECUTION : GradleBuildPerformanceMetric(
    readableString = "Start time of worker execution",
    type = ValueType.NANOSECONDS,
    name = "START_WORKER_EXECUTION"
)

object START_KOTLIN_DAEMON_EXECUTION : GradleBuildPerformanceMetric(
    readableString = "Start time of kotlin daemon task execution",
    type = ValueType.NANOSECONDS,
    name = "START_KOTLIN_DAEMON_EXECUTION"
)

enum class ValueType {
    BYTES,
    NUMBER,
    NANOSECONDS,
    MILLISECONDS,
    TIME
}