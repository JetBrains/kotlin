/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.api

import java.io.File
import java.io.Serializable
import java.util.*

// mostly copied from /compiler/daemon/daemon-common/src/org/jetbrains/kotlin/daemon/common/CompilationOptions.kt

enum class TargetPlatform : Serializable {
    JVM,
    JS,
    METADATA
}

open class KotlinCompilationOptions(
    val compilerMode: CompilerMode,
    val targetPlatform: TargetPlatform,
    /** @See [ReportCategory] */
    val reportCategories: Array<Int>,
    /** @See [ReportSeverity] */
    val reportSeverity: Int,
    /** @See [CompilationResultCategory]] */
    val requestedCompilationResults: Array<Int>,
    val kotlinScriptExtensions: Array<String>? = null
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }

    override fun toString(): String {
        return "CompilationOptions(" +
                "compilerMode=$compilerMode, " +
                "targetPlatform=$targetPlatform, " +
                "reportCategories=${Arrays.toString(reportCategories)}, " +
                "reportSeverity=$reportSeverity, " +
                "requestedCompilationResults=${Arrays.toString(requestedCompilationResults)}, " +
                "kotlinScriptExtensions=${Arrays.toString(kotlinScriptExtensions)}" +
                ")"
    }
}

class IncrementalKotlinCompilationOptions(
    val areFileChangesKnown: Boolean,
    val modifiedFiles: List<File>?,
    val deletedFiles: List<File>?,
    val classpathChanges: ClasspathChanges,
    val workingDir: File,
    compilerMode: CompilerMode,
    targetPlatform: TargetPlatform,
    /** @See [ReportCategory] */
    reportCategories: Array<Int>,
    /** @See [ReportSeverity] */
    reportSeverity: Int,
    /** @See [CompilationResultCategory]] */
    requestedCompilationResults: Array<Int>,
    val usePreciseJavaTracking: Boolean,
    /**
     * Directories that should be cleared when IC decides to rebuild
     */
    val outputFiles: List<File>,
    val multiModuleICSettings: MultiModuleICSettings,
    val modulesInfo: IncrementalModuleInfo,
    kotlinScriptExtensions: Array<String>? = null,
    val withAbiSnapshot: Boolean = false,
    val preciseCompilationResultsBackup: Boolean = false,
) : KotlinCompilationOptions(
    compilerMode,
    targetPlatform,
    reportCategories,
    reportSeverity,
    requestedCompilationResults,
    kotlinScriptExtensions
) {
    companion object {
        const val serialVersionUID: Long = 1
    }

    override fun toString(): String {
        return "IncrementalCompilationOptions(" +
                "super=${super.toString()}, " +
                "areFileChangesKnown=$areFileChangesKnown, " +
                "modifiedFiles=$modifiedFiles, " +
                "deletedFiles=$deletedFiles, " +
                "classpathChanges=${classpathChanges::class.simpleName}, " +
                "workingDir=$workingDir, " +
                "multiModuleICSettings=$multiModuleICSettings, " +
                "usePreciseJavaTracking=$usePreciseJavaTracking, " +
                "outputFiles=$outputFiles" +
                ")"
    }
}

data class MultiModuleICSettings(
    val buildHistoryFile: File,
    val useModuleDetection: Boolean
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}

enum class CompilerMode : Serializable {
    NON_INCREMENTAL_COMPILER,
    INCREMENTAL_COMPILER,
    JPS_COMPILER
}