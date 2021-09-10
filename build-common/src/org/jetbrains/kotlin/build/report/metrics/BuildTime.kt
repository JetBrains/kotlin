/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

@Suppress("Reformat")
enum class BuildTime(val parent: BuildTime? = null, val readableString: String) : Serializable {
    GRADLE_TASK(readableString = "Total Gradle task time"),
    GRADLE_TASK_ACTION(readableString = "Task action"),
        CLEAR_OUTPUT(GRADLE_TASK_ACTION, "Clear output"),
        BACKUP_OUTPUT(GRADLE_TASK_ACTION, "Backup output"),
        RESTORE_OUTPUT_FROM_BACKUP(GRADLE_TASK_ACTION, "Restore output"),
        CONNECT_TO_DAEMON(GRADLE_TASK_ACTION, "Connect to Kotlin daemon"),
        CLEAR_JAR_CACHE(GRADLE_TASK_ACTION, "Clear jar cache"),
        CALCULATE_OUTPUT_SIZE(GRADLE_TASK_ACTION, "Calculate output size"),
        RUN_COMPILATION(GRADLE_TASK_ACTION, "Run compilation"),
            NON_INCREMENTAL_COMPILATION_IN_PROCESS(RUN_COMPILATION, "Non incremental inprocess compilation"),
            NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS(RUN_COMPILATION, "Non incremental out of process compilation"),
            NON_INCREMENTAL_COMPILATION_DAEMON(RUN_COMPILATION, "Non incremental compilation in daemon"),
            INCREMENTAL_COMPILATION_DAEMON(RUN_COMPILATION, "Incremental compilation in daemon"),
                STORE_BUILD_INFO(INCREMENTAL_COMPILATION_DAEMON, "Store build info"),
                JAR_SNAPSHOT(INCREMENTAL_COMPILATION_DAEMON, "ABI JAR Snapshot support"),
                    SET_UP_ABI_SNAPSHOTS(JAR_SNAPSHOT, "Set up ABI snapshot"),
                    IC_ANALYZE_JAR_FILES(JAR_SNAPSHOT, "Analyze jar files"),
                IC_CALCULATE_INITIAL_DIRTY_SET(INCREMENTAL_COMPILATION_DAEMON, "Calculate initial dirty sources set"), //TODO
                    COMPUTE_CLASSPATH_CHANGES(IC_CALCULATE_INITIAL_DIRTY_SET, "Compute task classpath changes"),
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
                IC_WRITE_HISTORY_FILE(INCREMENTAL_COMPILATION_DAEMON, "Write history file"),
                SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION(INCREMENTAL_COMPILATION_DAEMON, "Shrink and save current classpath snapshot after compilation"),
                    LOAD_SHRUNK_PREVIOUS_CLASSPATH_SNAPSHOT_AFTER_COMPILATION(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Load shrunk previous classpath snapshot after compilation"),
                    LOAD_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Load current classpath snapshot after compilation"),
                    SHRINK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Shrink current classpath snapshot after compilation"),
                    SAVE_SHRUNK_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION(SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION, "Save shrunk classpath snapshot after compilation"),
    COMPILER_PERFORMANCE(readableString = "Compiler time"),
        COMPILER_INITIALIZATION(COMPILER_PERFORMANCE, "Compiler initialization time"),
        CODE_ANALYSIS(COMPILER_PERFORMANCE, "Compiler code analysis"),
        CODE_GENERATION(COMPILER_PERFORMANCE, "Compiler code generation"),
    ;

    companion object {
        const val serialVersionUID = 0L

        val children by lazy {
            values().filter { it.parent != null }.groupBy { it.parent }
        }
    }
}