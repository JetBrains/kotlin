/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

object GRADLE_TASK : BuildTimeMetric(readableString = "Total Gradle task time", name = "GRADLE_TASK")
object GRADLE_TASK_PREPARATION : BuildTimeMetric(readableString = "Spent time before task action", name = "GRADLE_TASK_PREPARATION")
object GRADLE_TASK_ACTION : BuildTimeMetric(readableString = "Task action", name = "GRADLE_TASK_ACTION")

val OUT_OF_WORKER_TASK_ACTION =
    GRADLE_TASK_ACTION.createChild("Task action before worker execution", name = "OUT_OF_WORKER_TASK_ACTION")

val BACKUP_OUTPUT = OUT_OF_WORKER_TASK_ACTION.createChild("Backup output", name = "BACKUP_OUTPUT")

object RUN_WORKER_DELAY : BuildTimeMetric(readableString = "Start gradle worker", name = "RUN_WORKER_DELAY")

val RUN_COMPILATION_IN_WORKER =
    GRADLE_TASK_ACTION.createChild("Run compilation in Gradle worker", name = "RUN_COMPILATION_IN_WORKER")

val CLEAR_JAR_CACHE = RUN_COMPILATION_IN_WORKER.createChild("Clear jar cache", name = "CLEAR_JAR_CACHE")
val CLEAR_OUTPUT = RUN_COMPILATION_IN_WORKER.createChild("Clear output", name = "CLEAR_OUTPUT")
val PRECISE_BACKUP_OUTPUT = RUN_COMPILATION_IN_WORKER.createChild("Precise backup output", name = "PRECISE_BACKUP_OUTPUT")
val RESTORE_OUTPUT_FROM_BACKUP = RUN_COMPILATION_IN_WORKER.createChild("Restore output", name = "RESTORE_OUTPUT_FROM_BACKUP")
val CLEAN_BACKUP_STASH = RUN_COMPILATION_IN_WORKER.createChild("Cleaning up the backup stash", name = "CLEAN_BACKUP_STASH")
val CONNECT_TO_DAEMON = RUN_COMPILATION_IN_WORKER.createChild("Connect to Kotlin daemon", name = "CONNECT_TO_DAEMON")
val CALCULATE_OUTPUT_SIZE = RUN_COMPILATION_IN_WORKER.createChild("Calculate output size", name = "CALCULATE_OUTPUT_SIZE")
val RUN_COMPILATION = RUN_COMPILATION_IN_WORKER.createChild("Run compilation", name = "RUN_COMPILATION")
val NATIVE_IN_PROCESS = RUN_COMPILATION.createChild("Run native in process", name = "NATIVE_IN_PROCESS")
val RUN_ENTRY_POINT = NATIVE_IN_PROCESS.createChild("Run entry point", name = "RUN_ENTRY_POINT")
val NATIVE_IN_EXECUTOR = RUN_COMPILATION.createChild("Run native in executor", name = "NATIVE_IN_EXECUTOR")
val NON_INCREMENTAL_COMPILATION_IN_PROCESS =
    RUN_COMPILATION.createChild("Non incremental inprocess compilation", name = "NON_INCREMENTAL_COMPILATION_IN_PROCESS")

val NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS =
    RUN_COMPILATION.createChild("Non incremental out of process compilation", name = "NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS")

val NON_INCREMENTAL_COMPILATION_DAEMON =
    RUN_COMPILATION.createChild("Non incremental compilation in daemon", name = "NON_INCREMENTAL_COMPILATION_DAEMON")

val INCREMENTAL_COMPILATION_DAEMON =
    RUN_COMPILATION.createChild("Incremental compilation in daemon", name = "INCREMENTAL_COMPILATION_DAEMON")

val STORE_BUILD_INFO = INCREMENTAL_COMPILATION_DAEMON.createChild("Store build info", name = "STORE_BUILD_INFO")
val JAR_SNAPSHOT = INCREMENTAL_COMPILATION_DAEMON.createChild("ABI JAR Snapshot support", name = "JAR_SNAPSHOT")
val SET_UP_ABI_SNAPSHOTS = JAR_SNAPSHOT.createChild("Set up ABI snapshot", name = "SET_UP_ABI_SNAPSHOTS")
val IC_ANALYZE_JAR_FILES = JAR_SNAPSHOT.createChild("Analyze jar files", name = "IC_ANALYZE_JAR_FILES")
val IC_CALCULATE_INITIAL_DIRTY_SET =
    INCREMENTAL_COMPILATION_DAEMON.createChild("Calculate initial dirty sources set", name = "IC_CALCULATE_INITIAL_DIRTY_SET") //TODO

val COMPUTE_CLASSPATH_CHANGES =
    IC_CALCULATE_INITIAL_DIRTY_SET.createChild("Compute classpath changes", name = "COMPUTE_CLASSPATH_CHANGES")

val LOAD_CURRENT_CLASSPATH_SNAPSHOT =
    COMPUTE_CLASSPATH_CHANGES.createChild("Load current classpath snapshot", name = "LOAD_CURRENT_CLASSPATH_SNAPSHOT")

val REMOVE_DUPLICATE_CLASSES =
    LOAD_CURRENT_CLASSPATH_SNAPSHOT.createChild("Remove duplicate classes", name = "REMOVE_DUPLICATE_CLASSES")

val SHRINK_CURRENT_CLASSPATH_SNAPSHOT =
    COMPUTE_CLASSPATH_CHANGES.createChild("Shrink current classpath snapshot", name = "SHRINK_CURRENT_CLASSPATH_SNAPSHOT")

val GET_LOOKUP_SYMBOLS = SHRINK_CURRENT_CLASSPATH_SNAPSHOT.createChild("Get lookup symbols", name = "GET_LOOKUP_SYMBOLS")
val FIND_REFERENCED_CLASSES =
    SHRINK_CURRENT_CLASSPATH_SNAPSHOT.createChild("Find referenced classes", name = "FIND_REFERENCED_CLASSES")

val FIND_TRANSITIVELY_REFERENCED_CLASSES = SHRINK_CURRENT_CLASSPATH_SNAPSHOT.createChild(
    "Find transitively referenced classes",
    name = "FIND_TRANSITIVELY_REFERENCED_CLASSES"
)

val LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT =
    COMPUTE_CLASSPATH_CHANGES.createChild("Load shrunk previous classpath snapshot", name = "LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT")

val COMPUTE_CHANGED_AND_IMPACTED_SET =
    COMPUTE_CLASSPATH_CHANGES.createChild("Compute changed and impacted set", name = "COMPUTE_CHANGED_AND_IMPACTED_SET")

val COMPUTE_CLASS_CHANGES = COMPUTE_CHANGED_AND_IMPACTED_SET.createChild("Compute class changes", name = "COMPUTE_CLASS_CHANGES")
val COMPUTE_KOTLIN_CLASS_CHANGES =
    COMPUTE_CLASS_CHANGES.createChild("Compute Kotlin class changes", name = "COMPUTE_KOTLIN_CLASS_CHANGES")

val COMPUTE_JAVA_CLASS_CHANGES =
    COMPUTE_CLASS_CHANGES.createChild("Compute Java class changes", name = "COMPUTE_JAVA_CLASS_CHANGES")

val COMPUTE_IMPACTED_SET = COMPUTE_CHANGED_AND_IMPACTED_SET.createChild("Compute impacted set", name = "COMPUTE_IMPACTED_SET")
val IC_ANALYZE_CHANGES_IN_DEPENDENCIES =
    IC_CALCULATE_INITIAL_DIRTY_SET.createChild("Analyze dependency changes", name = "IC_ANALYZE_CHANGES_IN_DEPENDENCIES")

val IC_FIND_HISTORY_FILES = IC_ANALYZE_CHANGES_IN_DEPENDENCIES.createChild("Find history files", name = "IC_FIND_HISTORY_FILES")
val IC_ANALYZE_HISTORY_FILES =
    IC_ANALYZE_CHANGES_IN_DEPENDENCIES.createChild("Analyze history files", name = "IC_ANALYZE_HISTORY_FILES")

val IC_ANALYZE_CHANGES_IN_JAVA_SOURCES =
    IC_CALCULATE_INITIAL_DIRTY_SET.createChild("Analyze Java file changes", name = "IC_ANALYZE_CHANGES_IN_JAVA_SOURCES")

val IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS =
    IC_CALCULATE_INITIAL_DIRTY_SET.createChild("Analyze Android layouts", name = "IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS")

val IC_DETECT_REMOVED_CLASSES =
    IC_CALCULATE_INITIAL_DIRTY_SET.createChild("Detect removed classes", name = "IC_DETECT_REMOVED_CLASSES")

val CLEAR_OUTPUT_ON_REBUILD =
    INCREMENTAL_COMPILATION_DAEMON.createChild("Clear outputs on rebuild", name = "CLEAR_OUTPUT_ON_REBUILD")

val IC_UPDATE_CACHES = INCREMENTAL_COMPILATION_DAEMON.createChild("Update caches", name = "IC_UPDATE_CACHES")
val COMPILATION_ROUND = INCREMENTAL_COMPILATION_DAEMON.createChild("Sources compilation round", name = "COMPILATION_ROUND")
val COMPILER_PERFORMANCE = COMPILATION_ROUND.createChild(readableString = "Compiler time", name = "COMPILER_PERFORMANCE")
val COMPILER_INITIALIZATION = COMPILER_PERFORMANCE.createChild("Compiler initialization time", name = "COMPILER_INITIALIZATION")
val CODE_ANALYSIS = COMPILER_PERFORMANCE.createChild("Compiler code analysis", name = "CODE_ANALYSIS")
val TRANSLATION_TO_IR = COMPILER_PERFORMANCE.createChild("Compiler translation to IR", name = "TRANSLATION_TO_IR")
val IR_PRE_LOWERING = COMPILER_PERFORMANCE.createChild("Compiler IR pre-lowering", name = "IR_PRE_LOWERING")
val IR_SERIALIZATION = COMPILER_PERFORMANCE.createChild("Compiler IR Serialization", name = "IR_SERIALIZATION")
val KLIB_WRITING = COMPILER_PERFORMANCE.createChild("Compiler Klib writing", name = "KLIB_WRITING")
val CODE_GENERATION = COMPILER_PERFORMANCE.createChild("Compiler code generation", name = "CODE_GENERATION")
val IR_LOWERING = CODE_GENERATION.createChild("Compiler IR lowering", name = "IR_LOWERING")
val BACKEND = CODE_GENERATION.createChild("Compiler backend", name = "BACKEND")
val IC_WRITE_HISTORY_FILE = INCREMENTAL_COMPILATION_DAEMON.createChild("Write history file", name = "IC_WRITE_HISTORY_FILE")
val SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION =
    INCREMENTAL_COMPILATION_DAEMON.createChild(
        "Shrink and save current classpath snapshot after compilation",
        name = "SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION"
    )

val INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT =
    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION.createChild(
        "Shrink current classpath snapshot incrementally",
        name = "INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT"
    )

val INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT =
    INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT.createChild(
        "Load current classpath snapshot",
        name = "INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT"
    )

val INCREMENTAL_REMOVE_DUPLICATE_CLASSES =
    INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT.createChild("Remove duplicate classes", name = "INCREMENTAL_REMOVE_DUPLICATE_CLASSES")

val INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS =
    INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT.createChild(
        "Load shrunk current classpath snapshot against previous lookups",
        name = "INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS"
    )

val NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT =
    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION.createChild(
        "Shrink current classpath snapshot non-incrementally",
        name = "NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT"
    )

val NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT =
    NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT.createChild(
        "Load current classpath snapshot",
        name = "NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT"
    )

val NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES =
    NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT.createChild(
        "Remove duplicate classes",
        name = "NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES"
    )

val SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT =
    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION.createChild(
        "Save shrunk current classpath snapshot",
        name = "SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT"
    )

object TASK_FINISH_LISTENER_NOTIFICATION :
    BuildTimeMetric(readableString = "Task finish event notification", name = "TASK_FINISH_LISTENER_NOTIFICATION")

object CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM :
    BuildTimeMetric(readableString = "Classpath entry snapshot transform", name = "CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM")

val LOAD_CLASSES_PATHS_ONLY = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM.createChild("Load classes (paths only)", name = "LOAD_CLASSES_PATHS_ONLY")

val SNAPSHOT_CLASSES = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM.createChild("Snapshot classes", name = "SNAPSHOT_CLASSES")
val LOAD_CONTENTS_OF_CLASSES = SNAPSHOT_CLASSES.createChild("Load contents of classes", name = "LOAD_CONTENTS_OF_CLASSES")
val SNAPSHOT_KOTLIN_CLASSES = SNAPSHOT_CLASSES.createChild("Snapshot Kotlin classes", name = "SNAPSHOT_KOTLIN_CLASSES")
val SNAPSHOT_JAVA_CLASSES = SNAPSHOT_CLASSES.createChild("Snapshot Java classes", name = "SNAPSHOT_JAVA_CLASSES")
val SNAPSHOT_INLINED_CLASSES = SNAPSHOT_CLASSES.createChild("Snapshot inlined classes", name = "SNAPSHOT_INLINED_CLASSES")
val SAVE_CLASSPATH_ENTRY_SNAPSHOT =
    CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM.createChild("Save classpath entry snapshot", name = "SAVE_CLASSPATH_ENTRY_SNAPSHOT")

