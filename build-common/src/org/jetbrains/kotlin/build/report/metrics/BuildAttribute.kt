/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

enum class BuildAttribute(val kind: BuildAttributeKind) : Serializable {
    NO_BUILD_HISTORY(BuildAttributeKind.REBUILD_REASON),
    CACHE_CORRUPTION(BuildAttributeKind.REBUILD_REASON),
    UNKNOWN_CHANGES_IN_GRADLE_INPUTS(BuildAttributeKind.REBUILD_REASON),
    JAVA_CHANGE_UNTRACKED_FILE_IS_REMOVED(BuildAttributeKind.REBUILD_REASON),
    JAVA_CHANGE_UNEXPECTED_PSI(BuildAttributeKind.REBUILD_REASON),
    JAVA_CHANGE_UNKNOWN_QUALIFIER(BuildAttributeKind.REBUILD_REASON),
    DEP_CHANGE_REMOVED_ENTRY(BuildAttributeKind.REBUILD_REASON),
    DEP_CHANGE_HISTORY_IS_NOT_FOUND(BuildAttributeKind.REBUILD_REASON),
    DEP_CHANGE_HISTORY_CANNOT_BE_READ(BuildAttributeKind.REBUILD_REASON),
    DEP_CHANGE_HISTORY_NO_KNOWN_BUILDS(BuildAttributeKind.REBUILD_REASON),
    DEP_CHANGE_NON_INCREMENTAL_BUILD_IN_DEP(BuildAttributeKind.REBUILD_REASON),
    IN_PROCESS_EXECUTION(BuildAttributeKind.REBUILD_REASON),
    OUT_OF_PROCESS_EXECUTION(BuildAttributeKind.REBUILD_REASON),
    IC_IS_NOT_ENABLED(BuildAttributeKind.REBUILD_REASON);

    companion object {
        const val serialVersionUID = 0L
    }
}
