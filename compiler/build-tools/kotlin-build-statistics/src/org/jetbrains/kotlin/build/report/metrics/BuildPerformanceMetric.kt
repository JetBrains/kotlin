/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

sealed class BuildPerformanceMetric(
    val name: String,
    val readableString: String,
    val type: ValueType,
    val parent: BuildPerformanceMetric? = null,
) : Serializable {
    // Ensure Kotlin object singletons remain singletons after Java deserialization
    private fun readResolve(): Any = this::class.objectInstance ?: this
}

sealed class JpsBuildPerformanceMetric(readableString: String, name: String, type: ValueType, parent: JpsBuildPerformanceMetric? = null) :
    BuildPerformanceMetric(name, readableString, type, parent)


object DAEMON_GC_TIME : JpsBuildPerformanceMetric(
    readableString = "Time spent in GC",
    name = "DAEMON_GC_TIME",
    type = ValueType.NANOSECONDS
) {
    private fun readResolve(): Any = DAEMON_GC_TIME
}

object DAEMON_GC_COUNT : JpsBuildPerformanceMetric(
    readableString = "Count of GC",
    name = "DAEMON_GC_COUNT",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = DAEMON_GC_COUNT
}

object JPS_COMPILE_ITERATION : JpsBuildPerformanceMetric(
    readableString = "Total compiler iteration",
    name = "COMPILE_ITERATION",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = JPS_COMPILE_ITERATION
}

object JPS_IC_COMPILE_ITERATION : JpsBuildPerformanceMetric(
    readableString = "Total kotlin compiler iteration",
    name = "IC_COMPILE_ITERATION",
    type = ValueType.NUMBER,
    parent = JPS_COMPILE_ITERATION
) {
    private fun readResolve(): Any = JPS_IC_COMPILE_ITERATION
}

object JPS_SOURCE_LINES_NUMBER : JpsBuildPerformanceMetric(
    readableString = "Number of lines analyzed",
    name = "SOURCE_LINES_NUMBER",
    type = ValueType.NUMBER,
    parent = JPS_COMPILE_ITERATION
) {
    private fun readResolve(): Any = JPS_SOURCE_LINES_NUMBER
}

object JPS_ANALYSIS_LPS : JpsBuildPerformanceMetric(
    readableString = "Analysis lines per second",
    name = "ANALYSIS_LPS",
    type = ValueType.NUMBER,
    parent = JPS_COMPILE_ITERATION
) {
    private fun readResolve(): Any = JPS_ANALYSIS_LPS
}

object JPS_CODE_GENERATION_LPS : JpsBuildPerformanceMetric(
    readableString = "Code generation lines per second",
    name = "CODE_GENERATION_LPS",
    type = ValueType.NUMBER,
    parent = JPS_COMPILE_ITERATION
) {
    private fun readResolve(): Any = JPS_CODE_GENERATION_LPS
}

sealed class GradleBuildPerformanceMetric(
    name: String,
    readableString: String,
    type: ValueType,
    parent: GradleBuildPerformanceMetric? = null
) :
    BuildPerformanceMetric(name, readableString, type, parent)

object COMPILE_ITERATION : GradleBuildPerformanceMetric(
    name = "COMPILE_ITERATION",
    readableString = "Total compiler iteration",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = COMPILE_ITERATION
}

object IC_COMPILE_ITERATION : GradleBuildPerformanceMetric(
    name = "IC_COMPILE_ITERATION",
    readableString = "Total kotlin compiler iteration",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = IC_COMPILE_ITERATION
}

object SOURCE_LINES_NUMBER : GradleBuildPerformanceMetric(
    name = "SOURCE_LINES_NUMBER",
    readableString = "Number of lines analyzed",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = SOURCE_LINES_NUMBER
}

object ANALYSIS_LPS : GradleBuildPerformanceMetric(
    name = "ANALYSIS_LPS",
    readableString = "Analysis lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = ANALYSIS_LPS
}

object CODE_GENERATION_LPS : GradleBuildPerformanceMetric(
    name = "CODE_GENERATION_LPS",
    readableString = "Code generation lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = CODE_GENERATION_LPS
}
object CACHE_DIRECTORY_SIZE : GradleBuildPerformanceMetric(
    name = "CACHE_DIRECTORY_SIZE",
    readableString = "Total size of the cache directory",
    type = ValueType.BYTES
) {
    private fun readResolve(): Any = CACHE_DIRECTORY_SIZE
}

object LOOKUP_SIZE : GradleBuildPerformanceMetric(
    name = "LOOKUP_SIZE",
    readableString = "Lookups size",
    type = ValueType.BYTES,
    parent = CACHE_DIRECTORY_SIZE
) {
    private fun readResolve(): Any = LOOKUP_SIZE
}

object SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    name = "SNAPSHOT_SIZE",
    readableString = "ABI snapshot size",
    type = ValueType.BYTES,
    parent = CACHE_DIRECTORY_SIZE
) {
    private fun readResolve(): Any = SNAPSHOT_SIZE
}

object BUNDLE_SIZE : GradleBuildPerformanceMetric(
    name = "BUNDLE_SIZE",
    readableString = "Total size of the final bundle",
    type = ValueType.BYTES
) {
    private fun readResolve(): Any = BUNDLE_SIZE
}

object DAEMON_INCREASED_MEMORY : GradleBuildPerformanceMetric(
    name = "DAEMON_INCREASED_MEMORY",
    readableString = "Increase memory usage",
    type = ValueType.BYTES
) {
    private fun readResolve(): Any = DAEMON_INCREASED_MEMORY
}

object DAEMON_MEMORY_USAGE : GradleBuildPerformanceMetric(
    name = "DAEMON_MEMORY_USAGE",
    readableString = "Total memory usage at the end of build",
    type = ValueType.BYTES
) {
    private fun readResolve(): Any = DAEMON_MEMORY_USAGE
}

object TRANSLATION_TO_IR_LPS : GradleBuildPerformanceMetric(
    name = "TRANSLATION_TO_IR_LPS",
    readableString = "Translation to IR lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = TRANSLATION_TO_IR_LPS
}

object IR_PRE_LOWERING_LPS : GradleBuildPerformanceMetric(
    name = "IR_PRE_LOWERING_LPS",
    readableString = "IR pre-lowering lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = IR_PRE_LOWERING_LPS
}

object IR_SERIALIZATION_LPS : GradleBuildPerformanceMetric(
    name = "IR_SERIALIZATION_LPS",
    readableString = "IR serialization lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = IR_SERIALIZATION_LPS
}

object KLIB_WRITING_LPS : GradleBuildPerformanceMetric(
    name = "KLIB_WRITING_LPS",
    readableString = "KLib Writing lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = KLIB_WRITING_LPS
}

object IR_LOWERING_LPS : GradleBuildPerformanceMetric(
    name = "IR_LOWERING_LPS",
    readableString = "IR Lowering lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = IR_LOWERING_LPS
}

object BACKEND_LPS : GradleBuildPerformanceMetric(
    name = "BACKEND_LPS",
    readableString = "Backend lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = BACKEND_LPS
}

// Metrics for the `kotlin.incremental.useClasspathSnapshot` feature
object CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    name = "CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT",
    readableString = "Number of times 'ClasspathEntrySnapshotTransform' ran",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT
}

object JAR_CLASSPATH_ENTRY_SIZE : GradleBuildPerformanceMetric(
    name = "JAR_CLASSPATH_ENTRY_SIZE",
    readableString = "Size of jar classpath entry",
    type = ValueType.BYTES,
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT
) {
    private fun readResolve(): Any = JAR_CLASSPATH_ENTRY_SIZE
}

object JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    name = "JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE",
    readableString = "Size of jar classpath entry's snapshot",
    type = ValueType.BYTES,
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT
) {
    private fun readResolve(): Any = JAR_CLASSPATH_ENTRY_SNAPSHOT_SIZE
}

object DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    name = "DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE",
    readableString = "Size of directory classpath entry's snapshot",
    type = ValueType.BYTES,
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_EXECUTION_COUNT
) {
    private fun readResolve(): Any = DIRECTORY_CLASSPATH_ENTRY_SNAPSHOT_SIZE
}

object COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    name = "COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT",
    readableString = "Number of times classpath changes are computed",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = COMPUTE_CLASSPATH_CHANGES_EXECUTION_COUNT
}

object SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    name = "SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT",
    readableString = "Number of times classpath snapshot is shrunk and saved after compilation",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
}

object CLASSPATH_ENTRY_COUNT : GradleBuildPerformanceMetric(
    name = "CLASSPATH_ENTRY_COUNT",
    readableString = "Number of classpath entries",
    type = ValueType.NUMBER,
    parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
) {
    private fun readResolve(): Any = CLASSPATH_ENTRY_COUNT
}

object CLASSPATH_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    name = "CLASSPATH_SNAPSHOT_SIZE",
    readableString = "Size of classpath snapshot",
    type = ValueType.BYTES,
    parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
) {
    private fun readResolve(): Any = CLASSPATH_SNAPSHOT_SIZE
}

object SHRUNK_CLASSPATH_SNAPSHOT_SIZE : GradleBuildPerformanceMetric(
    name = "SHRUNK_CLASSPATH_SNAPSHOT_SIZE",
    readableString = "Size of shrunk classpath snapshot",
    type = ValueType.BYTES,
    parent = SHRINK_AND_SAVE_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
) {
    private fun readResolve(): Any = SHRUNK_CLASSPATH_SNAPSHOT_SIZE
}

object LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT : GradleBuildPerformanceMetric(
    name = "LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT",
    readableString = "Number of times classpath snapshot is loaded",
    type = ValueType.NUMBER
) {
    private fun readResolve(): Any = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
}

object LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS : GradleBuildPerformanceMetric(
    name = "LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS",
    readableString = "Number of cache hits when loading classpath entry snapshots",
    type = ValueType.NUMBER,
    parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
) {
    private fun readResolve(): Any = LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS
}

object LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES : GradleBuildPerformanceMetric(
    name = "LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES",
    readableString = "Number of cache misses when loading classpath entry snapshots",
    type = ValueType.NUMBER,
    parent = LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT
) {
    private fun readResolve(): Any = LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES
}

//time metrics
object START_TASK_ACTION_EXECUTION : GradleBuildPerformanceMetric(
    name = "START_TASK_ACTION_EXECUTION",
    readableString = "Start time of task action",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = START_TASK_ACTION_EXECUTION
}

object FINISH_KOTLIN_DAEMON_EXECUTION : GradleBuildPerformanceMetric(
    name = "FINISH_KOTLIN_DAEMON_EXECUTION",
    readableString = "Finish time of kotlin daemon execution",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = FINISH_KOTLIN_DAEMON_EXECUTION
}

object CALL_KOTLIN_DAEMON : GradleBuildPerformanceMetric(
    name = "CALL_KOTLIN_DAEMON",
    readableString = "Finish gradle part of task execution",
    type = ValueType.NANOSECONDS
) {
    private fun readResolve(): Any = CALL_KOTLIN_DAEMON
}

object CALL_WORKER : GradleBuildPerformanceMetric(
    name = "CALL_WORKER",
    readableString = "Worker submit time",
    type = ValueType.NANOSECONDS
) {
    private fun readResolve(): Any = CALL_WORKER
}

object START_WORKER_EXECUTION : GradleBuildPerformanceMetric(
    name = "START_WORKER_EXECUTION",
    readableString = "Start time of worker execution",
    type = ValueType.NANOSECONDS
) {
    private fun readResolve(): Any = START_WORKER_EXECUTION
}

object START_KOTLIN_DAEMON_EXECUTION : GradleBuildPerformanceMetric(
    name = "START_KOTLIN_DAEMON_EXECUTION",
    readableString = "Start time of kotlin daemon task execution",
    type = ValueType.NANOSECONDS
) {
    private fun readResolve(): Any = START_KOTLIN_DAEMON_EXECUTION
}

enum class ValueType {
    BYTES,
    NUMBER,
    NANOSECONDS,
    MILLISECONDS,
    TIME
}