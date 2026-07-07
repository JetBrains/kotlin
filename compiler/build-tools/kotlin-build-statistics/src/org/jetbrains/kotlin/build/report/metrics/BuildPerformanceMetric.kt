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

object IR_LINKING_LPS : GradleBuildPerformanceMetric(
    name = "IR_LINKING_LPS",
    readableString = "IR Linking lines per second",
    type = ValueType.NUMBER,
    parent = COMPILE_ITERATION
) {
    private fun readResolve(): Any = IR_LINKING_LPS
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

// Start time metrics for GradleBuildTimeMetric
object GRADLE_TASK_START : GradleBuildPerformanceMetric(
    name = "GRADLE_TASK_START",
    readableString = "Start time of total Gradle task",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = GRADLE_TASK_START
}

object GRADLE_TASK_PREPARATION_START : GradleBuildPerformanceMetric(
    name = "GRADLE_TASK_PREPARATION_START",
    readableString = "Start time of spent time before task action",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = GRADLE_TASK_PREPARATION_START
}

object GRADLE_TASK_ACTION_START : GradleBuildPerformanceMetric(
    name = "GRADLE_TASK_ACTION_START",
    readableString = "Start time of task action",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = GRADLE_TASK_ACTION_START
}

object OUT_OF_WORKER_TASK_ACTION_START : GradleBuildPerformanceMetric(
    name = "OUT_OF_WORKER_TASK_ACTION_START",
    readableString = "Start time of task action before worker execution",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = OUT_OF_WORKER_TASK_ACTION_START
}

object BACKUP_OUTPUT_START : GradleBuildPerformanceMetric(
    name = "BACKUP_OUTPUT_START",
    readableString = "Start time of backup output",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = BACKUP_OUTPUT_START
}

object RUN_WORKER_DELAY_START : GradleBuildPerformanceMetric(
    name = "RUN_WORKER_DELAY_START",
    readableString = "Start time of start gradle worker",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = RUN_WORKER_DELAY_START
}

object RUN_COMPILATION_IN_WORKER_START : GradleBuildPerformanceMetric(
    name = "RUN_COMPILATION_IN_WORKER_START",
    readableString = "Start time of run compilation in Gradle worker",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = RUN_COMPILATION_IN_WORKER_START
}

object CLEAR_JAR_CACHE_START : GradleBuildPerformanceMetric(
    name = "CLEAR_JAR_CACHE_START",
    readableString = "Start time of clear jar cache",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CLEAR_JAR_CACHE_START
}

object CLEAR_OUTPUT_START : GradleBuildPerformanceMetric(
    name = "CLEAR_OUTPUT_START",
    readableString = "Start time of clear output",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CLEAR_OUTPUT_START
}

object PRECISE_BACKUP_OUTPUT_START : GradleBuildPerformanceMetric(
    name = "PRECISE_BACKUP_OUTPUT_START",
    readableString = "Start time of precise backup output",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = PRECISE_BACKUP_OUTPUT_START
}

object RESTORE_OUTPUT_FROM_BACKUP_START : GradleBuildPerformanceMetric(
    name = "RESTORE_OUTPUT_FROM_BACKUP_START",
    readableString = "Start time of restore output",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = RESTORE_OUTPUT_FROM_BACKUP_START
}

object CLEAN_BACKUP_STASH_START : GradleBuildPerformanceMetric(
    name = "CLEAN_BACKUP_STASH_START",
    readableString = "Start time of cleaning up the backup stash",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CLEAN_BACKUP_STASH_START
}

object CONNECT_TO_DAEMON_START : GradleBuildPerformanceMetric(
    name = "CONNECT_TO_DAEMON_START",
    readableString = "Start time of connect to Kotlin daemon",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CONNECT_TO_DAEMON_START
}

object CALCULATE_OUTPUT_SIZE_START : GradleBuildPerformanceMetric(
    name = "CALCULATE_OUTPUT_SIZE_START",
    readableString = "Start time of calculate output size",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CALCULATE_OUTPUT_SIZE_START
}

object RUN_COMPILATION_START : GradleBuildPerformanceMetric(
    name = "RUN_COMPILATION_START",
    readableString = "Start time of run compilation",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = RUN_COMPILATION_START
}

object NATIVE_IN_PROCESS_START : GradleBuildPerformanceMetric(
    name = "NATIVE_IN_PROCESS_START",
    readableString = "Start time of run native in process",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NATIVE_IN_PROCESS_START
}

object RUN_ENTRY_POINT_START : GradleBuildPerformanceMetric(
    name = "RUN_ENTRY_POINT_START",
    readableString = "Start time of run entry point",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = RUN_ENTRY_POINT_START
}

object NATIVE_IN_EXECUTOR_START : GradleBuildPerformanceMetric(
    name = "NATIVE_IN_EXECUTOR_START",
    readableString = "Start time of run native in executor",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NATIVE_IN_EXECUTOR_START
}

object NON_INCREMENTAL_COMPILATION_IN_PROCESS_START : GradleBuildPerformanceMetric(
    name = "NON_INCREMENTAL_COMPILATION_IN_PROCESS_START",
    readableString = "Start time of non incremental inprocess compilation",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NON_INCREMENTAL_COMPILATION_IN_PROCESS_START
}

object NON_INCREMENTAL_COMPILATION_DAEMON_START : GradleBuildPerformanceMetric(
    name = "NON_INCREMENTAL_COMPILATION_DAEMON_START",
    readableString = "Start time of non incremental compilation in daemon",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NON_INCREMENTAL_COMPILATION_DAEMON_START
}

object INCREMENTAL_COMPILATION_DAEMON_START : GradleBuildPerformanceMetric(
    name = "INCREMENTAL_COMPILATION_DAEMON_START",
    readableString = "Start time of incremental compilation in daemon",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = INCREMENTAL_COMPILATION_DAEMON_START
}

object STORE_BUILD_INFO_START : GradleBuildPerformanceMetric(
    name = "STORE_BUILD_INFO_START",
    readableString = "Start time of store build info",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = STORE_BUILD_INFO_START
}

object JAR_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "JAR_SNAPSHOT_START",
    readableString = "Start time of ABI JAR Snapshot support",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = JAR_SNAPSHOT_START
}

object SET_UP_ABI_SNAPSHOTS_START : GradleBuildPerformanceMetric(
    name = "SET_UP_ABI_SNAPSHOTS_START",
    readableString = "Start time of set up ABI snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SET_UP_ABI_SNAPSHOTS_START
}

object IC_ANALYZE_JAR_FILES_START : GradleBuildPerformanceMetric(
    name = "IC_ANALYZE_JAR_FILES_START",
    readableString = "Start time of analyze jar files",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_ANALYZE_JAR_FILES_START
}

object IC_CALCULATE_INITIAL_DIRTY_SET_START : GradleBuildPerformanceMetric(
    name = "IC_CALCULATE_INITIAL_DIRTY_SET_START",
    readableString = "Start time of calculate initial dirty sources set",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_CALCULATE_INITIAL_DIRTY_SET_START
}

object COMPUTE_CLASSPATH_CHANGES_START : GradleBuildPerformanceMetric(
    name = "COMPUTE_CLASSPATH_CHANGES_START",
    readableString = "Start time of compute classpath changes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPUTE_CLASSPATH_CHANGES_START
}

object LOAD_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "LOAD_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of load current classpath snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = LOAD_CURRENT_CLASSPATH_SNAPSHOT_START
}

object REMOVE_DUPLICATE_CLASSES_START : GradleBuildPerformanceMetric(
    name = "REMOVE_DUPLICATE_CLASSES_START",
    readableString = "Start time of remove duplicate classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = REMOVE_DUPLICATE_CLASSES_START
}

object SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of shrink current classpath snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START
}

object GET_LOOKUP_SYMBOLS_START : GradleBuildPerformanceMetric(
    name = "GET_LOOKUP_SYMBOLS_START",
    readableString = "Start time of get lookup symbols",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = GET_LOOKUP_SYMBOLS_START
}

object FIND_REFERENCED_CLASSES_START : GradleBuildPerformanceMetric(
    name = "FIND_REFERENCED_CLASSES_START",
    readableString = "Start time of find referenced classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = FIND_REFERENCED_CLASSES_START
}

object FIND_TRANSITIVELY_REFERENCED_CLASSES_START : GradleBuildPerformanceMetric(
    name = "FIND_TRANSITIVELY_REFERENCED_CLASSES_START",
    readableString = "Start time of find transitively referenced classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = FIND_TRANSITIVELY_REFERENCED_CLASSES_START
}

object LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of load shrunk previous classpath snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT_START
}

object COMPUTE_CHANGED_AND_IMPACTED_SET_START : GradleBuildPerformanceMetric(
    name = "COMPUTE_CHANGED_AND_IMPACTED_SET_START",
    readableString = "Start time of compute changed and impacted set",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPUTE_CHANGED_AND_IMPACTED_SET_START
}

object COMPUTE_CLASS_CHANGES_START : GradleBuildPerformanceMetric(
    name = "COMPUTE_CLASS_CHANGES_START",
    readableString = "Start time of compute class changes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPUTE_CLASS_CHANGES_START
}

object COMPUTE_KOTLIN_CLASS_CHANGES_START : GradleBuildPerformanceMetric(
    name = "COMPUTE_KOTLIN_CLASS_CHANGES_START",
    readableString = "Start time of compute Kotlin class changes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPUTE_KOTLIN_CLASS_CHANGES_START
}

object COMPUTE_JAVA_CLASS_CHANGES_START : GradleBuildPerformanceMetric(
    name = "COMPUTE_JAVA_CLASS_CHANGES_START",
    readableString = "Start time of compute Java class changes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPUTE_JAVA_CLASS_CHANGES_START
}

object COMPUTE_IMPACTED_SET_START : GradleBuildPerformanceMetric(
    name = "COMPUTE_IMPACTED_SET_START",
    readableString = "Start time of compute impacted set",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPUTE_IMPACTED_SET_START
}

object IC_ANALYZE_CHANGES_IN_DEPENDENCIES_START : GradleBuildPerformanceMetric(
    name = "IC_ANALYZE_CHANGES_IN_DEPENDENCIES_START",
    readableString = "Start time of analyze dependency changes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_ANALYZE_CHANGES_IN_DEPENDENCIES_START
}

object IC_FIND_HISTORY_FILES_START : GradleBuildPerformanceMetric(
    name = "IC_FIND_HISTORY_FILES_START",
    readableString = "Start time of find history files",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_FIND_HISTORY_FILES_START
}

object IC_ANALYZE_HISTORY_FILES_START : GradleBuildPerformanceMetric(
    name = "IC_ANALYZE_HISTORY_FILES_START",
    readableString = "Start time of analyze history files",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_ANALYZE_HISTORY_FILES_START
}

object IC_ANALYZE_CHANGES_IN_JAVA_SOURCES_START : GradleBuildPerformanceMetric(
    name = "IC_ANALYZE_CHANGES_IN_JAVA_SOURCES_START",
    readableString = "Start time of analyze Java file changes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_ANALYZE_CHANGES_IN_JAVA_SOURCES_START
}

object IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS_START : GradleBuildPerformanceMetric(
    name = "IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS_START",
    readableString = "Start time of analyze Android layouts",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS_START
}

object IC_DETECT_REMOVED_CLASSES_START : GradleBuildPerformanceMetric(
    name = "IC_DETECT_REMOVED_CLASSES_START",
    readableString = "Start time of detect removed classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_DETECT_REMOVED_CLASSES_START
}

object CLEAR_OUTPUT_ON_REBUILD_START : GradleBuildPerformanceMetric(
    name = "CLEAR_OUTPUT_ON_REBUILD_START",
    readableString = "Start time of clear outputs on rebuild",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CLEAR_OUTPUT_ON_REBUILD_START
}

object IC_GEN_COMPILER_REF_INDEX_START : GradleBuildPerformanceMetric(
    name = "IC_GEN_COMPILER_REF_INDEX_START",
    readableString = "Start time of generate compiler reference index",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_GEN_COMPILER_REF_INDEX_START
}

object IC_UPDATE_CACHES_START : GradleBuildPerformanceMetric(
    name = "IC_UPDATE_CACHES_START",
    readableString = "Start time of update caches",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_UPDATE_CACHES_START
}

object COMPILATION_ROUND_START : GradleBuildPerformanceMetric(
    name = "COMPILATION_ROUND_START",
    readableString = "Start time of sources compilation round",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPILATION_ROUND_START
}

object COMPILER_PERFORMANCE_START : GradleBuildPerformanceMetric(
    name = "COMPILER_PERFORMANCE_START",
    readableString = "Start time of compiler time",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPILER_PERFORMANCE_START
}

object COMPILER_INITIALIZATION_START : GradleBuildPerformanceMetric(
    name = "COMPILER_INITIALIZATION_START",
    readableString = "Start time of compiler initialization time",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = COMPILER_INITIALIZATION_START
}

object CODE_ANALYSIS_START : GradleBuildPerformanceMetric(
    name = "CODE_ANALYSIS_START",
    readableString = "Start time of compiler code analysis",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CODE_ANALYSIS_START
}

object TRANSLATION_TO_IR_START : GradleBuildPerformanceMetric(
    name = "TRANSLATION_TO_IR_START",
    readableString = "Start time of compiler translation to IR",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = TRANSLATION_TO_IR_START
}

object IR_PRE_LOWERING_START : GradleBuildPerformanceMetric(
    name = "IR_PRE_LOWERING_START",
    readableString = "Start time of compiler IR pre-lowering",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IR_PRE_LOWERING_START
}

object IR_SERIALIZATION_START : GradleBuildPerformanceMetric(
    name = "IR_SERIALIZATION_START",
    readableString = "Start time of compiler IR Serialization",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IR_SERIALIZATION_START
}

object KLIB_WRITING_START : GradleBuildPerformanceMetric(
    name = "KLIB_WRITING_START",
    readableString = "Start time of compiler Klib writing",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = KLIB_WRITING_START
}

object IR_LINKING_START : GradleBuildPerformanceMetric(
    name = "IR_LINKING_START",
    readableString = "Start time of compiler IR linking",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IR_LINKING_START
}

object CODE_GENERATION_START : GradleBuildPerformanceMetric(
    name = "CODE_GENERATION_START",
    readableString = "Start time of compiler code generation",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CODE_GENERATION_START
}

object IR_LOWERING_START : GradleBuildPerformanceMetric(
    name = "IR_LOWERING_START",
    readableString = "Start time of compiler IR lowering",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IR_LOWERING_START
}

object BACKEND_START : GradleBuildPerformanceMetric(
    name = "BACKEND_START",
    readableString = "Start time of compiler backend",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = BACKEND_START
}

object IC_WRITE_HISTORY_FILE_START : GradleBuildPerformanceMetric(
    name = "IC_WRITE_HISTORY_FILE_START",
    readableString = "Start time of write history file",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = IC_WRITE_HISTORY_FILE_START
}

object SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION_START : GradleBuildPerformanceMetric(
    name = "SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION_START",
    readableString = "Start time of shrink and save current classpath snapshot after compilation",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION_START
}

object INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of shrink current classpath snapshot incrementally",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START
}

object INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of load current classpath snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT_START
}

object INCREMENTAL_REMOVE_DUPLICATE_CLASSES_START : GradleBuildPerformanceMetric(
    name = "INCREMENTAL_REMOVE_DUPLICATE_CLASSES_START",
    readableString = "Start time of remove duplicate classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = INCREMENTAL_REMOVE_DUPLICATE_CLASSES_START
}

object INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS_START : GradleBuildPerformanceMetric(
    name = "INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS_START",
    readableString = "Start time of load shrunk current classpath snapshot against previous lookups",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS_START
}

object NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of shrink current classpath snapshot non-incrementally",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT_START
}

object NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of load current classpath snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT_START
}

object NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES_START : GradleBuildPerformanceMetric(
    name = "NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES_START",
    readableString = "Start time of remove duplicate classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES_START
}

object SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_START",
    readableString = "Start time of save shrunk current classpath snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_START
}

object TASK_FINISH_LISTENER_NOTIFICATION_START : GradleBuildPerformanceMetric(
    name = "TASK_FINISH_LISTENER_NOTIFICATION_START",
    readableString = "Start time of task finish event notification",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = TASK_FINISH_LISTENER_NOTIFICATION_START
}

object CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_START : GradleBuildPerformanceMetric(
    name = "CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_START",
    readableString = "Start time of classpath entry snapshot transform",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM_START
}

object LOAD_CLASSES_PATHS_ONLY_START : GradleBuildPerformanceMetric(
    name = "LOAD_CLASSES_PATHS_ONLY_START",
    readableString = "Start time of load classes (paths only)",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = LOAD_CLASSES_PATHS_ONLY_START
}

object SNAPSHOT_CLASSES_START : GradleBuildPerformanceMetric(
    name = "SNAPSHOT_CLASSES_START",
    readableString = "Start time of snapshot classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SNAPSHOT_CLASSES_START
}

object LOAD_CONTENTS_OF_CLASSES_START : GradleBuildPerformanceMetric(
    name = "LOAD_CONTENTS_OF_CLASSES_START",
    readableString = "Start time of load contents of classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = LOAD_CONTENTS_OF_CLASSES_START
}

object SNAPSHOT_KOTLIN_CLASSES_START : GradleBuildPerformanceMetric(
    name = "SNAPSHOT_KOTLIN_CLASSES_START",
    readableString = "Start time of snapshot Kotlin classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SNAPSHOT_KOTLIN_CLASSES_START
}

object SNAPSHOT_JAVA_CLASSES_START : GradleBuildPerformanceMetric(
    name = "SNAPSHOT_JAVA_CLASSES_START",
    readableString = "Start time of snapshot Java classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SNAPSHOT_JAVA_CLASSES_START
}

object SNAPSHOT_INLINED_CLASSES_START : GradleBuildPerformanceMetric(
    name = "SNAPSHOT_INLINED_CLASSES_START",
    readableString = "Start time of snapshot inlined classes",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SNAPSHOT_INLINED_CLASSES_START
}

object SAVE_CLASSPATH_ENTRY_SNAPSHOT_START : GradleBuildPerformanceMetric(
    name = "SAVE_CLASSPATH_ENTRY_SNAPSHOT_START",
    readableString = "Start time of save classpath entry snapshot",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = SAVE_CLASSPATH_ENTRY_SNAPSHOT_START
}

object GRADLE_CONFIGURATION_TIME_START : GradleBuildPerformanceMetric(
    name = "GRADLE_CONFIGURATION_TIME_START",
    readableString = "Start time of Gradle configuration time",
    type = ValueType.TIME
) {
    private fun readResolve(): Any = GRADLE_CONFIGURATION_TIME_START
}

enum class ValueType {
    BYTES,
    NUMBER,
    NANOSECONDS,
    MILLISECONDS,
    TIME
}