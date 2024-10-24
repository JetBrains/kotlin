/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.IncrementalCompilationFeatures
import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import java.io.File
import java.io.Serializable
import java.util.*

open class CompilationOptions(
        val compilerMode: CompilerMode,
        val targetPlatform: CompileService.TargetPlatform,
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

class IncrementalCompilationOptions(
    val sourceChanges: ChangedFiles,
    val classpathChanges: ClasspathChanges,
    val workingDir: File,
    compilerMode: CompilerMode,
    targetPlatform: CompileService.TargetPlatform,
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
    val outputFiles: Collection<File>? = null,
    val multiModuleICSettings: MultiModuleICSettings? = null,
    val modulesInfo: IncrementalModuleInfo? = null,

    // rootProjectDir and buildDir are used to resolve relative paths
    val rootProjectDir: File?,
    val buildDir: File?,

    kotlinScriptExtensions: Array<String>? = null,
    val icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
) : CompilationOptions(
    compilerMode,
    targetPlatform,
    reportCategories,
    reportSeverity,
    requestedCompilationResults,
    kotlinScriptExtensions
) {
    constructor(
        areFileChangesKnown: Boolean,
        modifiedFiles: List<File>?,
        deletedFiles: List<File>?,
        classpathChanges: ClasspathChanges,
        workingDir: File,
        compilerMode: CompilerMode,
        targetPlatform: CompileService.TargetPlatform,
        /** @See [ReportCategory] */
        reportCategories: Array<Int>,
        /** @See [ReportSeverity] */
        reportSeverity: Int,
        /** @See [CompilationResultCategory]] */
        requestedCompilationResults: Array<Int>,
        usePreciseJavaTracking: Boolean,
        /**
         * Directories that should be cleared when IC decides to rebuild
         */
        outputFiles: Collection<File>? = null,
        multiModuleICSettings: MultiModuleICSettings? = null,
        modulesInfo: IncrementalModuleInfo? = null,

        // rootProjectDir and buildDir are used to resolve relative paths
        rootProjectDir: File?,
        buildDir: File?,

        kotlinScriptExtensions: Array<String>? = null,
        withAbiSnapshot: Boolean = false,
        preciseCompilationResultsBackup: Boolean = false,
        keepIncrementalCompilationCachesInMemory: Boolean = false,
    ) : this(
        if (areFileChangesKnown) ChangedFiles.DeterminableFiles.Known(modifiedFiles!!, deletedFiles!!) else ChangedFiles.Unknown(),
        classpathChanges,
        workingDir,
        compilerMode,
        targetPlatform,
        reportCategories,
        reportSeverity,
        requestedCompilationResults,
        usePreciseJavaTracking,
        outputFiles,
        multiModuleICSettings,
        modulesInfo,
        rootProjectDir,
        buildDir,
        kotlinScriptExtensions,
        withAbiSnapshot,
        preciseCompilationResultsBackup,
        keepIncrementalCompilationCachesInMemory,
    )

    companion object {
        const val serialVersionUID: Long = 4
    }

    override fun toString(): String {
        val (modifiedFiles, deletedFiles) = when (sourceChanges) {
            is ChangedFiles.DeterminableFiles.Known -> {
                listOf(sourceChanges.modified, sourceChanges.removed)
            }
            else -> {
                listOf(null, null)
            }
        }
        return "IncrementalCompilationOptions(" +
                "super=${super.toString()}, " +
                "areFileChangesKnown=${sourceChanges::class.simpleName}, " +
                "modifiedFiles=$modifiedFiles, " +
                "deletedFiles=$deletedFiles, " +
                "classpathChanges=${classpathChanges::class.simpleName}, " +
                "workingDir=$workingDir, " +
                "multiModuleICSettings=$multiModuleICSettings, " +
                "usePreciseJavaTracking=$usePreciseJavaTracking, " +
                "icFeatures=$icFeatures, " +
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
