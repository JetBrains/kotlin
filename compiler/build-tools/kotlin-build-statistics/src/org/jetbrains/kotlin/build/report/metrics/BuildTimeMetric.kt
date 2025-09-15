/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

sealed class BuildTimeMetric(parent: BuildTimeMetric?, readableString: String, name: String) :
    BuildMetric<BuildTimeMetric>(parent, readableString, name) {

    constructor(readableString: String, name: String) : this(null, readableString, name)
}


sealed class JpsBuildTimeMetric(parent: JpsBuildTimeMetric? = null, readableString: String, name: String) :
    BuildTimeMetric(parent, readableString, name)

object JPS_ITERATION : JpsBuildTimeMetric(readableString = "Jps iteration", name = "JPS_ITERATION")

object JPS_COMPILATION_ROUND :
    JpsBuildTimeMetric(parent = JPS_ITERATION, readableString = "Sources compilation round", name = "COMPILATION_ROUND")

object JPS_COMPILER_PERFORMANCE :
    JpsBuildTimeMetric(parent = JPS_COMPILATION_ROUND, readableString = "Compiler time", name = "COMPILER_PERFORMANCE")

object JPS_COMPILER_INITIALIZATION :
    JpsBuildTimeMetric(parent = JPS_COMPILER_PERFORMANCE, readableString = "Compiler initialization time", name = "COMPILER_INITIALIZATION")

object JPS_CODE_ANALYSIS :
    JpsBuildTimeMetric(parent = JPS_COMPILER_PERFORMANCE, readableString = "Compiler code analysis", name = "CODE_ANALYSIS")

object JPS_CODE_GENERATION :
    JpsBuildTimeMetric(parent = JPS_COMPILER_PERFORMANCE, readableString = "Compiler code generation", name = "CODE_GENERATION")


sealed class GradleBuildTimeMetric(parent: GradleBuildTimeMetric? = null, readableString: String, name: String) :
    BuildTimeMetric(parent, readableString, name)

object GRADLE_TASK : GradleBuildTimeMetric(readableString = "Total Gradle task time", name = "GRADLE_TASK")
object GRADLE_TASK_PREPARATION : GradleBuildTimeMetric(readableString = "Spent time before task action", name = "GRADLE_TASK_PREPARATION")
object GRADLE_TASK_ACTION : GradleBuildTimeMetric(readableString = "Task action", name = "GRADLE_TASK_ACTION")
object OUT_OF_WORKER_TASK_ACTION :
    GradleBuildTimeMetric(GRADLE_TASK_ACTION, "Task action before worker execution", name = "OUT_OF_WORKER_TASK_ACTION")

object BACKUP_OUTPUT : GradleBuildTimeMetric(OUT_OF_WORKER_TASK_ACTION, "Backup output", name = "BACKUP_OUTPUT")
object RUN_WORKER_DELAY : GradleBuildTimeMetric(readableString = "Start gradle worker", name = "RUN_WORKER_DELAY")
object RUN_COMPILATION_IN_WORKER :
    GradleBuildTimeMetric(GRADLE_TASK_ACTION, "Run compilation in Gradle worker", name = "RUN_COMPILATION_IN_WORKER")

object CLEAR_JAR_CACHE : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Clear jar cache", name = "CLEAR_JAR_CACHE")
object CLEAR_OUTPUT : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Clear output", name = "CLEAR_OUTPUT")
object PRECISE_BACKUP_OUTPUT : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Precise backup output", name = "PRECISE_BACKUP_OUTPUT")
object RESTORE_OUTPUT_FROM_BACKUP : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Restore output", name = "RESTORE_OUTPUT_FROM_BACKUP")
object CLEAN_BACKUP_STASH : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Cleaning up the backup stash", name = "CLEAN_BACKUP_STASH")
object CONNECT_TO_DAEMON : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Connect to Kotlin daemon", name = "CONNECT_TO_DAEMON")
object CALCULATE_OUTPUT_SIZE : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Calculate output size", name = "CALCULATE_OUTPUT_SIZE")
object RUN_COMPILATION : GradleBuildTimeMetric(RUN_COMPILATION_IN_WORKER, "Run compilation", name = "RUN_COMPILATION")
object NATIVE_IN_PROCESS : GradleBuildTimeMetric(RUN_COMPILATION, "Run native in process", name = "NATIVE_IN_PROCESS")
object RUN_ENTRY_POINT : GradleBuildTimeMetric(NATIVE_IN_PROCESS, "Run entry point", name = "RUN_ENTRY_POINT")
object NATIVE_IN_EXECUTOR : GradleBuildTimeMetric(RUN_COMPILATION, "Run native in executor", name = "NATIVE_IN_EXECUTOR")
object NON_INCREMENTAL_COMPILATION_IN_PROCESS :
    GradleBuildTimeMetric(RUN_COMPILATION, "Non incremental inprocess compilation", name = "NON_INCREMENTAL_COMPILATION_IN_PROCESS")

object NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS : GradleBuildTimeMetric(
    RUN_COMPILATION,
    "Non incremental out of process compilation",
    name = "NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS"
)

object NON_INCREMENTAL_COMPILATION_DAEMON :
    GradleBuildTimeMetric(RUN_COMPILATION, "Non incremental compilation in daemon", name = "NON_INCREMENTAL_COMPILATION_DAEMON")

object INCREMENTAL_COMPILATION_DAEMON :
    GradleBuildTimeMetric(RUN_COMPILATION, "Incremental compilation in daemon", name = "INCREMENTAL_COMPILATION_DAEMON")

object STORE_BUILD_INFO : GradleBuildTimeMetric(INCREMENTAL_COMPILATION_DAEMON, "Store build info", name = "STORE_BUILD_INFO")
object JAR_SNAPSHOT : GradleBuildTimeMetric(INCREMENTAL_COMPILATION_DAEMON, "ABI JAR Snapshot support", name = "JAR_SNAPSHOT")
object SET_UP_ABI_SNAPSHOTS : GradleBuildTimeMetric(JAR_SNAPSHOT, "Set up ABI snapshot", name = "SET_UP_ABI_SNAPSHOTS")
object IC_ANALYZE_JAR_FILES : GradleBuildTimeMetric(JAR_SNAPSHOT, "Analyze jar files", name = "IC_ANALYZE_JAR_FILES")
object IC_CALCULATE_INITIAL_DIRTY_SET : GradleBuildTimeMetric(
    INCREMENTAL_COMPILATION_DAEMON,
    "Calculate initial dirty sources set",
    name = "IC_CALCULATE_INITIAL_DIRTY_SET"
)

object COMPUTE_CLASSPATH_CHANGES :
    GradleBuildTimeMetric(IC_CALCULATE_INITIAL_DIRTY_SET, "Compute classpath changes", name = "COMPUTE_CLASSPATH_CHANGES")

object LOAD_CURRENT_CLASSPATH_SNAPSHOT :
    GradleBuildTimeMetric(COMPUTE_CLASSPATH_CHANGES, "Load current classpath snapshot", name = "LOAD_CURRENT_CLASSPATH_SNAPSHOT")

object REMOVE_DUPLICATE_CLASSES :
    GradleBuildTimeMetric(LOAD_CURRENT_CLASSPATH_SNAPSHOT, "Remove duplicate classes", name = "REMOVE_DUPLICATE_CLASSES")

object SHRINK_CURRENT_CLASSPATH_SNAPSHOT :
    GradleBuildTimeMetric(COMPUTE_CLASSPATH_CHANGES, "Shrink current classpath snapshot", name = "SHRINK_CURRENT_CLASSPATH_SNAPSHOT")

object GET_LOOKUP_SYMBOLS : GradleBuildTimeMetric(SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Get lookup symbols", name = "GET_LOOKUP_SYMBOLS")
object FIND_REFERENCED_CLASSES :
    GradleBuildTimeMetric(SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Find referenced classes", name = "FIND_REFERENCED_CLASSES")

object FIND_TRANSITIVELY_REFERENCED_CLASSES : GradleBuildTimeMetric(
    SHRINK_CURRENT_CLASSPATH_SNAPSHOT,
    "Find transitively referenced classes",
    name = "FIND_TRANSITIVELY_REFERENCED_CLASSES"
)

object LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT : GradleBuildTimeMetric(
    COMPUTE_CLASSPATH_CHANGES,
    "Load shrunk previous classpath snapshot",
    name = "LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT"
)

object COMPUTE_CHANGED_AND_IMPACTED_SET :
    GradleBuildTimeMetric(COMPUTE_CLASSPATH_CHANGES, "Compute changed and impacted set", name = "COMPUTE_CHANGED_AND_IMPACTED_SET")

object COMPUTE_CLASS_CHANGES :
    GradleBuildTimeMetric(COMPUTE_CHANGED_AND_IMPACTED_SET, "Compute class changes", name = "COMPUTE_CLASS_CHANGES")

object COMPUTE_KOTLIN_CLASS_CHANGES :
    GradleBuildTimeMetric(COMPUTE_CLASS_CHANGES, "Compute Kotlin class changes", name = "COMPUTE_KOTLIN_CLASS_CHANGES")

object COMPUTE_JAVA_CLASS_CHANGES :
    GradleBuildTimeMetric(COMPUTE_CLASS_CHANGES, "Compute Java class changes", name = "COMPUTE_JAVA_CLASS_CHANGES")

object COMPUTE_IMPACTED_SET : GradleBuildTimeMetric(COMPUTE_CHANGED_AND_IMPACTED_SET, "Compute impacted set", name = "COMPUTE_IMPACTED_SET")
object IC_ANALYZE_CHANGES_IN_DEPENDENCIES :
    GradleBuildTimeMetric(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze dependency changes", name = "IC_ANALYZE_CHANGES_IN_DEPENDENCIES")

object IC_FIND_HISTORY_FILES :
    GradleBuildTimeMetric(IC_ANALYZE_CHANGES_IN_DEPENDENCIES, "Find history files", name = "IC_FIND_HISTORY_FILES")

object IC_ANALYZE_HISTORY_FILES :
    GradleBuildTimeMetric(IC_ANALYZE_CHANGES_IN_DEPENDENCIES, "Analyze history files", name = "IC_ANALYZE_HISTORY_FILES")

object IC_ANALYZE_CHANGES_IN_JAVA_SOURCES :
    GradleBuildTimeMetric(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze Java file changes", name = "IC_ANALYZE_CHANGES_IN_JAVA_SOURCES")

object IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS :
    GradleBuildTimeMetric(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze Android layouts", name = "IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS")

object IC_DETECT_REMOVED_CLASSES :
    GradleBuildTimeMetric(IC_CALCULATE_INITIAL_DIRTY_SET, "Detect removed classes", name = "IC_DETECT_REMOVED_CLASSES")

object CLEAR_OUTPUT_ON_REBUILD :
    GradleBuildTimeMetric(INCREMENTAL_COMPILATION_DAEMON, "Clear outputs on rebuild", name = "CLEAR_OUTPUT_ON_REBUILD")

object IC_UPDATE_CACHES : GradleBuildTimeMetric(INCREMENTAL_COMPILATION_DAEMON, "Update caches", name = "IC_UPDATE_CACHES")
object COMPILATION_ROUND : GradleBuildTimeMetric(INCREMENTAL_COMPILATION_DAEMON, "Sources compilation round", name = "COMPILATION_ROUND")
object COMPILER_PERFORMANCE : GradleBuildTimeMetric(COMPILATION_ROUND, readableString = "Compiler time", name = "COMPILER_PERFORMANCE")
object COMPILER_INITIALIZATION :
    GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler initialization time", name = "COMPILER_INITIALIZATION")

object CODE_ANALYSIS : GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler code analysis", name = "CODE_ANALYSIS")
object TRANSLATION_TO_IR : GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler translation to IR", name = "TRANSLATION_TO_IR")
object IR_PRE_LOWERING : GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler IR pre-lowering", name = "IR_PRE_LOWERING")
object IR_SERIALIZATION : GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler IR Serialization", name = "IR_SERIALIZATION")
object KLIB_WRITING : GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler Klib writing", name = "KLIB_WRITING")
object CODE_GENERATION : GradleBuildTimeMetric(COMPILER_PERFORMANCE, "Compiler code generation", name = "CODE_GENERATION")
object IR_LOWERING : GradleBuildTimeMetric(CODE_GENERATION, "Compiler IR lowering", name = "IR_LOWERING")
object BACKEND : GradleBuildTimeMetric(CODE_GENERATION, "Compiler backend", name = "BACKEND")
object IC_WRITE_HISTORY_FILE : GradleBuildTimeMetric(INCREMENTAL_COMPILATION_DAEMON, "Write history file", name = "IC_WRITE_HISTORY_FILE")
object SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION : GradleBuildTimeMetric(
    INCREMENTAL_COMPILATION_DAEMON,
    "Shrink and save current classpath snapshot after compilation",
    name = "SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION"
)

object INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT : GradleBuildTimeMetric(
    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION,
    "Shrink current classpath snapshot incrementally",
    name = "INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT"
)

object INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT : GradleBuildTimeMetric(
    INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT,
    "Load current classpath snapshot",
    name = "INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT"
)

object INCREMENTAL_REMOVE_DUPLICATE_CLASSES : GradleBuildTimeMetric(
    INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT,
    "Remove duplicate classes",
    name = "INCREMENTAL_REMOVE_DUPLICATE_CLASSES"
)

object INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS : GradleBuildTimeMetric(
    INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT,
    "Load shrunk current classpath snapshot against previous lookups",
    name = "INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS"
)

object NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT : GradleBuildTimeMetric(
    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION,
    "Shrink current classpath snapshot non-incrementally",
    name = "NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT"
)

object NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT : GradleBuildTimeMetric(
    NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT,
    "Load current classpath snapshot",
    name = "NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT"
)

object NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES : GradleBuildTimeMetric(
    NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT,
    "Remove duplicate classes",
    name = "NON_INCREMENTAL_REMOVE_DUPLICATE_CLASSES"
)

object SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT : GradleBuildTimeMetric(
    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION,
    "Save shrunk current classpath snapshot",
    name = "SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT"
)

object TASK_FINISH_LISTENER_NOTIFICATION :
    GradleBuildTimeMetric(readableString = "Task finish event notification", name = "TASK_FINISH_LISTENER_NOTIFICATION")

object CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM :
    GradleBuildTimeMetric(readableString = "Classpath entry snapshot transform", name = "CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM")

object LOAD_CLASSES_PATHS_ONLY :
    GradleBuildTimeMetric(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM, "Load classes (paths only)", name = "LOAD_CLASSES_PATHS_ONLY")

object SNAPSHOT_CLASSES : GradleBuildTimeMetric(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM, "Snapshot classes", name = "SNAPSHOT_CLASSES")
object LOAD_CONTENTS_OF_CLASSES :
    GradleBuildTimeMetric(parent = SNAPSHOT_CLASSES, "Load contents of classes", name = "LOAD_CONTENTS_OF_CLASSES")

object SNAPSHOT_KOTLIN_CLASSES :
    GradleBuildTimeMetric(parent = SNAPSHOT_CLASSES, "Snapshot Kotlin classes", name = "SNAPSHOT_KOTLIN_CLASSES")

object SNAPSHOT_JAVA_CLASSES : GradleBuildTimeMetric(parent = SNAPSHOT_CLASSES, "Snapshot Java classes", name = "SNAPSHOT_JAVA_CLASSES")
object SNAPSHOT_INLINED_CLASSES :
    GradleBuildTimeMetric(parent = SNAPSHOT_CLASSES, "Snapshot inlined classes", name = "SNAPSHOT_INLINED_CLASSES")

object SAVE_CLASSPATH_ENTRY_SNAPSHOT : GradleBuildTimeMetric(
    parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM,
    "Save classpath entry snapshot",
    name = "SAVE_CLASSPATH_ENTRY_SNAPSHOT"
)
