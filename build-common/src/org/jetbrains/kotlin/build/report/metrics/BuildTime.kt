/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

@Suppress("Reformat")
enum class BuildTime(val parent: BuildTime? = null, val readableString: String) : Serializable {
    GRADLE_TASK_ACTION(readableString = "Task execution"),
    GRADLE_TASK(readableString = "Total time"),
        CLEAR_OUTPUT(GRADLE_TASK, "Clear output"),
        BACKUP_OUTPUT(GRADLE_TASK, "Backup output"),
        RESTORE_OUTPUT_FROM_BACKUP(GRADLE_TASK, "Restore output"),
        CONNECT_TO_DAEMON(GRADLE_TASK, "Connect to Kotlin daemon"),
        CLEAR_JAR_CACHE(GRADLE_TASK, "Clear jar cache"),
        RUN_COMPILER(GRADLE_TASK, "Run compiler"),
            NON_INCREMENTAL_COMPILATION_IN_PROCESS(RUN_COMPILER, "Inprocess compilation"),
            NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS(RUN_COMPILER, "Out of process compilation"),
            NON_INCREMENTAL_COMPILATION_DAEMON(RUN_COMPILER, "Non incremental compilation"),
            INCREMENTAL_COMPILATION(RUN_COMPILER, "Incremental compilation"),
                STORE_BUILD_INFO(INCREMENTAL_COMPILATION, "Store build info"),
                JAR_SNAPSHOT(INCREMENTAL_COMPILATION, "ABI JAR Snapshot support"),
                    SET_UP_ABI_SNAPSHOTS(JAR_SNAPSHOT, "Set up ABI snapshot"),
                    IC_ANALYZE_JAR_FILES(JAR_SNAPSHOT, "Analyze jar files"),
                IC_CALCULATE_INITIAL_DIRTY_SET(INCREMENTAL_COMPILATION, "Init dirty symbols set"),
                    IC_ANALYZE_CHANGES_IN_DEPENDENCIES(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze dependency changes"),
                        IC_FIND_HISTORY_FILES(IC_ANALYZE_CHANGES_IN_DEPENDENCIES, "Find history files"),
                        IC_ANALYZE_HISTORY_FILES(IC_ANALYZE_CHANGES_IN_DEPENDENCIES, "Analyze history files"),
                    IC_ANALYZE_CHANGES_IN_JAVA_SOURCES(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze Java file changes"),
                    IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS(IC_CALCULATE_INITIAL_DIRTY_SET, "Analyze Android layouts"),
                    IC_DETECT_REMOVED_CLASSES(IC_CALCULATE_INITIAL_DIRTY_SET, "Detect removed classes"),
                CLEAR_OUTPUT_ON_REBUILD(INCREMENTAL_COMPILATION, "Clear outputs on rebuild"),
                IC_UPDATE_CACHES(INCREMENTAL_COMPILATION, "Update caches"),
                INCREMENTAL_ITERATION(INCREMENTAL_COMPILATION, "Incremental iteration"),
                NON_INCREMENTAL_ITERATION(INCREMENTAL_COMPILATION, "Non-incremental iteration"),
                IC_WRITE_HISTORY_FILE(INCREMENTAL_COMPILATION, "Write history file");

    companion object {
        const val serialVersionUID = 0L

        val children by lazy {
            values().filter { it.parent != null }.groupBy { it.parent }
        }
    }
}