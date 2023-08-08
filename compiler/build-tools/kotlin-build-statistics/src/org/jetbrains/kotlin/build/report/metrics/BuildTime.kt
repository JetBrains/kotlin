/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

interface BuildTime : Serializable {
    fun getReadableString(): String

    fun getParent(): BuildTime?

    fun getAllMetrics(): List<BuildTime>

    fun children(): List<BuildTime>?

    fun getName(): String
}


enum class JpsBuildTime(private val parent: JpsBuildTime? = null, private val readableString: String) : BuildTime {

    JPS_ITERATION(readableString = "Jps iteration")
    ;

    override fun getReadableString(): String = readableString
    override fun getParent(): BuildTime? = parent

    override fun getAllMetrics(): List<BuildTime> {
        return entries
    }

    override fun children(): List<BuildTime>? {
        return children[this]
    }

    override fun getName(): String = this.name

    companion object {
        const val serialVersionUID = 1L

        val children by lazy {
            entries.filter { it.parent != null }.groupBy { it.parent }
        }
    }

}

@Suppress("Reformat")
enum class GradleBuildTime(private val parent: GradleBuildTime? = null, private val readableString: String) : BuildTime {
    // We use tabs to organize the hierarchy of metrics,
    // that's why the formatter is disabled.
    // @formatter:off
    GRADLE_TASK(readableString = "Total Gradle task time"),
    GRADLE_TASK_PREPARATION(readableString = "Spent time before task action"),
    GRADLE_TASK_ACTION(readableString = "Task action"),
        OUT_OF_WORKER_TASK_ACTION(GRADLE_TASK_ACTION, "Task action before worker execution"),
            BACKUP_OUTPUT(OUT_OF_WORKER_TASK_ACTION, "Backup output"),
        RUN_WORKER_DELAY(readableString = "Start gradle worker"),
        RUN_COMPILATION_IN_WORKER(GRADLE_TASK_ACTION, "Run compilation in Gradle worker"),
            CLEAR_JAR_CACHE(RUN_COMPILATION_IN_WORKER, "Clear jar cache"),
            CLEAR_OUTPUT(RUN_COMPILATION_IN_WORKER, "Clear output"),
            PRECISE_BACKUP_OUTPUT(RUN_COMPILATION_IN_WORKER, "Precise backup output"),
            RESTORE_OUTPUT_FROM_BACKUP(RUN_COMPILATION_IN_WORKER, "Restore output"),
            CLEAN_BACKUP_STASH(RUN_COMPILATION_IN_WORKER, "Cleaning up the backup stash"),
            CONNECT_TO_DAEMON(RUN_COMPILATION_IN_WORKER, "Connect to Kotlin daemon"),
            CALCULATE_OUTPUT_SIZE(RUN_COMPILATION_IN_WORKER, "Calculate output size"),
            RUN_COMPILATION(RUN_COMPILATION_IN_WORKER, "Run compilation"),
                NATIVE_IN_PROCESS(RUN_COMPILATION, "Run native in process"),
                    RUN_ENTRY_POINT(NATIVE_IN_PROCESS, "Run entry point"),
                NATIVE_IN_EXECUTOR(RUN_COMPILATION, "Run native in executor"),
                NON_INCREMENTAL_COMPILATION_IN_PROCESS(RUN_COMPILATION, "Non incremental inprocess compilation"),
                NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS(RUN_COMPILATION, "Non incremental out of process compilation"),
                NON_INCREMENTAL_COMPILATION_DAEMON(RUN_COMPILATION, "Non incremental compilation in daemon"),
                INCREMENTAL_COMPILATION_DAEMON(RUN_COMPILATION, "Incremental compilation in daemon"),
                    STORE_BUILD_INFO(INCREMENTAL_COMPILATION_DAEMON, "Store build info"),
                    JAR_SNAPSHOT(INCREMENTAL_COMPILATION_DAEMON, "ABI JAR Snapshot support"),
                        SET_UP_ABI_SNAPSHOTS(JAR_SNAPSHOT, "Set up ABI snapshot"),
                        IC_ANALYZE_JAR_FILES(JAR_SNAPSHOT, "Analyze jar files"),
                    IC_CALCULATE_INITIAL_DIRTY_SET(INCREMENTAL_COMPILATION_DAEMON, "Calculate initial dirty sources set"), //TODO
                        COMPUTE_CLASSPATH_CHANGES(IC_CALCULATE_INITIAL_DIRTY_SET, "Compute classpath changes"),
                            LOAD_CURRENT_CLASSPATH_SNAPSHOT(COMPUTE_CLASSPATH_CHANGES, "Load current classpath snapshot"),
                                REMOVE_DUPLICATE_CLASSES(LOAD_CURRENT_CLASSPATH_SNAPSHOT, "Remove duplicate classes"),
                            SHRINK_CURRENT_CLASSPATH_SNAPSHOT(COMPUTE_CLASSPATH_CHANGES, "Shrink current classpath snapshot"),
                                GET_LOOKUP_SYMBOLS(SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Get lookup symbols"),
                                FIND_REFERENCED_CLASSES(SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Find referenced classes"),
                                FIND_TRANSITIVELY_REFERENCED_CLASSES(SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Find transitively referenced classes"),
                            LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT(COMPUTE_CLASSPATH_CHANGES, "Load shrunk previous classpath snapshot"),
                            COMPUTE_CHANGED_AND_IMPACTED_SET(COMPUTE_CLASSPATH_CHANGES, "Compute changed and impacted set"),
                                COMPUTE_CLASS_CHANGES(COMPUTE_CHANGED_AND_IMPACTED_SET, "Compute class changes"),
                                    COMPUTE_KOTLIN_CLASS_CHANGES(COMPUTE_CLASS_CHANGES, "Compute Kotlin class changes"),
                                    COMPUTE_JAVA_CLASS_CHANGES(COMPUTE_CLASS_CHANGES, "Compute Java class changes"),
                                COMPUTE_IMPACTED_SET(COMPUTE_CHANGED_AND_IMPACTED_SET, "Compute impacted set"),
                        IC_ANALYZE_CHANGES_IN_DEPENDENCIES(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze dependency changes"),
                            IC_FIND_HISTORY_FILES(IC_ANALYZE_CHANGES_IN_DEPENDENCIES, "Find history files"),
                            IC_ANALYZE_HISTORY_FILES(IC_ANALYZE_CHANGES_IN_DEPENDENCIES, "Analyze history files"),
                        IC_ANALYZE_CHANGES_IN_JAVA_SOURCES(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze Java file changes"),
                        IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze Android layouts"),
                        IC_DETECT_REMOVED_CLASSES(IC_CALCULATE_INITIAL_DIRTY_SET, "Detect removed classes"),
                    CLEAR_OUTPUT_ON_REBUILD(INCREMENTAL_COMPILATION_DAEMON, "Clear outputs on rebuild"),
                    IC_UPDATE_CACHES(INCREMENTAL_COMPILATION_DAEMON, "Update caches"),
                    COMPILATION_ROUND(INCREMENTAL_COMPILATION_DAEMON, "Sources compilation round"),
                        COMPILER_PERFORMANCE(COMPILATION_ROUND, readableString = "Compiler time"),
                            COMPILER_INITIALIZATION(COMPILER_PERFORMANCE, "Compiler initialization time"),
                            CODE_ANALYSIS(COMPILER_PERFORMANCE, "Compiler code analysis"),
                            CODE_GENERATION(COMPILER_PERFORMANCE, "Compiler code generation"),
                    IC_WRITE_HISTORY_FILE(INCREMENTAL_COMPILATION_DAEMON, "Write history file"),
                    SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION(INCREMENTAL_COMPILATION_DAEMON, "Shrink and save current classpath snapshot after compilation"),
                        INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Shrink current classpath snapshot incrementally"),
                            INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT(INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Load current classpath snapshot"),
                            INCREMENTAL_LOAD_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AGAINST_PREVIOUS_LOOKUPS(INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Load shrunk current classpath snapshot against previous lookups"),
                        NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Shrink current classpath snapshot non-incrementally"),
                            NON_INCREMENTAL_LOAD_CURRENT_CLASSPATH_SNAPSHOT(NON_INCREMENTAL_SHRINK_CURRENT_CLASSPATH_SNAPSHOT, "Load current classpath snapshot"),
                        SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Save shrunk current classpath snapshot"),
    TASK_FINISH_LISTENER_NOTIFICATION(readableString = "Task finish event notification"),
    CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM(readableString = "Classpath entry snapshot transform"),
        LOAD_CLASSES_PATHS_ONLY(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM, "Load classes (paths only)"),
        SNAPSHOT_CLASSES(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM, "Snapshot classes"),
            LOAD_CONTENTS_OF_CLASSES(parent = SNAPSHOT_CLASSES, "Load contents of classes"),
            SNAPSHOT_KOTLIN_CLASSES(parent = SNAPSHOT_CLASSES, "Snapshot Kotlin classes"),
            SNAPSHOT_JAVA_CLASSES(parent = SNAPSHOT_CLASSES, "Snapshot Java classes"),
        SAVE_CLASSPATH_ENTRY_SNAPSHOT(parent = CLASSPATH_ENTRY_SNAPSHOT_TRANSFORM, "Save classpath entry snapshot"),
    ;
    // @formatter:on

    override fun getReadableString(): String = readableString
    override fun getParent(): BuildTime? = parent

    override fun children(): List<BuildTime>? {
        return children[this]
    }

    override fun getAllMetrics(): List<BuildTime> {
        return entries
    }

    override fun getName(): String = this.name

    companion object {
        const val serialVersionUID = 1L

        val children by lazy {
            entries.filter { it.parent != null }.groupBy { it.parent }
        }
    }
}