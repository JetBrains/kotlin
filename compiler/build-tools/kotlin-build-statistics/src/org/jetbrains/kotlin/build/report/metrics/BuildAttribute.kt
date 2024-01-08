/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable

enum class BuildAttributeKind : Serializable {
    REBUILD_REASON;

    companion object {
        const val serialVersionUID = 0L
    }
}

enum class BuildAttribute(val kind: BuildAttributeKind, val readableString: String) : Serializable {
    NO_BUILD_HISTORY(BuildAttributeKind.REBUILD_REASON, "Build history file not found"),
    NO_ABI_SNAPSHOT(BuildAttributeKind.REBUILD_REASON, "ABI snapshot not found"),
    NO_LAST_BUILD_INFO(BuildAttributeKind.REBUILD_REASON, "Last build info not found"),
    INVALID_LAST_BUILD_INFO(BuildAttributeKind.REBUILD_REASON, "Last build info corrupted"),
    CLASSPATH_SNAPSHOT_NOT_FOUND(BuildAttributeKind.REBUILD_REASON, "Classpath snapshot not found"),
    IC_FAILED_TO_GET_CHANGED_FILES(BuildAttributeKind.REBUILD_REASON, "Failed to get changed files"),
    IC_FAILED_TO_COMPUTE_FILES_TO_RECOMPILE(BuildAttributeKind.REBUILD_REASON, "Failed to compute files to recompile"),
    IC_FAILED_TO_COMPILE_INCREMENTALLY(BuildAttributeKind.REBUILD_REASON, "Failed to compile incrementally"),
    IC_FAILED_TO_CLOSE_CACHES(BuildAttributeKind.REBUILD_REASON, "Failed to close caches"),
    UNKNOWN_CHANGES_IN_GRADLE_INPUTS(BuildAttributeKind.REBUILD_REASON, "Unknown Gradle changes"),
    JAVA_CHANGE_UNTRACKED_FILE_IS_REMOVED(BuildAttributeKind.REBUILD_REASON, "Untracked Java file is removed"),
    JAVA_CHANGE_UNEXPECTED_PSI(BuildAttributeKind.REBUILD_REASON, "Java PSI file is expected"),
    JAVA_CHANGE_UNKNOWN_QUALIFIER(BuildAttributeKind.REBUILD_REASON, "Unknown Java qualifier name"),
    DEP_CHANGE_REMOVED_ENTRY(BuildAttributeKind.REBUILD_REASON, "Jar file is removed form dependency"),
    DEP_CHANGE_HISTORY_IS_NOT_FOUND(BuildAttributeKind.REBUILD_REASON, "Dependency history not found"),
    DEP_CHANGE_HISTORY_CANNOT_BE_READ(BuildAttributeKind.REBUILD_REASON, "Dependency history can not be read"),
    DEP_CHANGE_HISTORY_NO_KNOWN_BUILDS(BuildAttributeKind.REBUILD_REASON, "Dependency history id not available"),
    DEP_CHANGE_NON_INCREMENTAL_BUILD_IN_DEP(BuildAttributeKind.REBUILD_REASON, "Non incremental build in history"),
    IN_PROCESS_EXECUTION(BuildAttributeKind.REBUILD_REASON, "In-process execution"),
    OUT_OF_PROCESS_EXECUTION(BuildAttributeKind.REBUILD_REASON, "Out of process execution"),
    IC_IS_NOT_ENABLED(BuildAttributeKind.REBUILD_REASON, "Incremental compilation is not enabled"),
    UNSAFE_INCREMENTAL_CHANGE_KT_62686(BuildAttributeKind.REBUILD_REASON, "Incremental compilation might be incorrect (KT-62686)");

    companion object {
        const val serialVersionUID = 1L
    }
}
