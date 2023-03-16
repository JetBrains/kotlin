/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.api

import java.io.File
import java.util.*

// mostly copied from /compiler/daemon/daemon-common/src/org/jetbrains/kotlin/daemon/common/CompilationOptions.kt

enum class TargetPlatform {
    JVM,
    JS,
    METADATA
}

sealed class LaunchOptions {
    class Daemon(
        val mainClassName: String,
        val classpath: List<File>,
        val jvmArguments: List<String>,
        val launcher: KotlinCompilerLauncher,
    ) : LaunchOptions() {

    }

    object InProcess : LaunchOptions() {

    }
}

sealed class CompilationOptions(
    val compilerMode: CompilerMode,
    val targetPlatform: TargetPlatform,
    /** @See [ReportCategory] */
    val reportCategories: Array<Int>,
    /** @See [ReportSeverity] */
    val reportSeverity: Int,
    /** @See [CompilationResultCategory]] */
    val requestedCompilationResults: Array<Int>,
    val kotlinScriptExtensions: Array<String>?
) {
    class NonIncremental(
        compilerMode: CompilerMode,
        targetPlatform: TargetPlatform,
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>,
        kotlinScriptExtensions: Array<String>? = null
    ) : CompilationOptions(compilerMode, targetPlatform, reportCategories, reportSeverity, requestedCompilationResults, kotlinScriptExtensions) {
    }

    class Incremental(
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
    ) : CompilationOptions(compilerMode, targetPlatform, reportCategories, reportSeverity, requestedCompilationResults, kotlinScriptExtensions) {

    }
}

data class MultiModuleICSettings(
    val buildHistoryFile: File,
    val useModuleDetection: Boolean
)

enum class CompilerMode {
    NON_INCREMENTAL_COMPILER,
    INCREMENTAL_COMPILER,
    JPS_COMPILER
}