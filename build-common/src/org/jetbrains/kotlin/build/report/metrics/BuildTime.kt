/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

@Suppress("Reformat")
enum class BuildTime(val parent: BuildTime? = null) : Serializable {
    GRADLE_TASK,
        CLEAR_OUTPUT(GRADLE_TASK),
        BACKUP_OUTPUT(GRADLE_TASK),
        RESTORE_OUTPUT_FROM_BACKUP(GRADLE_TASK),
        CONNECT_TO_DAEMON(GRADLE_TASK),
        CLEAR_JAR_CACHE(GRADLE_TASK),
        RUN_COMPILER(GRADLE_TASK),
            NON_INCREMENTAL_COMPILATION_IN_PROCESS(RUN_COMPILER),
            NON_INCREMENTAL_COMPILATION_OUT_OF_PROCESS(RUN_COMPILER),
            NON_INCREMENTAL_COMPILATION_DAEMON(RUN_COMPILER),
            INCREMENTAL_COMPILATION(RUN_COMPILER),
                IC_CALCULATE_INITIAL_DIRTY_SET(INCREMENTAL_COMPILATION),
                    IC_ANALYZE_CHANGES_IN_DEPENDENCIES(IC_CALCULATE_INITIAL_DIRTY_SET),
                        IC_FIND_HISTORY_FILES(IC_ANALYZE_CHANGES_IN_DEPENDENCIES),
                        IC_ANALYZE_HISTORY_FILES(IC_ANALYZE_CHANGES_IN_DEPENDENCIES),
                    IC_ANALYZE_CHANGES_IN_JAVA_SOURCES(IC_CALCULATE_INITIAL_DIRTY_SET),
                    IC_ANALYZE_CHANGES_IN_ANDROID_LAYOUTS(IC_CALCULATE_INITIAL_DIRTY_SET),
                    IC_DETECT_REMOVED_CLASSES(IC_CALCULATE_INITIAL_DIRTY_SET),
                CLEAR_OUTPUT_ON_REBUILD(INCREMENTAL_COMPILATION),
                IC_UPDATE_CACHES(INCREMENTAL_COMPILATION),
                INCREMENTAL_ITERATION(INCREMENTAL_COMPILATION),
                NON_INCREMENTAL_ITERATION(INCREMENTAL_COMPILATION),
                IC_WRITE_HISTORY_FILE(INCREMENTAL_COMPILATION);

    companion object {
        const val serialVersionUID = 0L

        val children by lazy {
            values().filter { it.parent != null }.groupBy { it.parent }
        }
    }
}