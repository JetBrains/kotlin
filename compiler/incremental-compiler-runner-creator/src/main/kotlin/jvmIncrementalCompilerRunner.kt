/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.api.ClasspathChanges
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import java.io.File

fun createIncrementalJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter,
    buildHistoryFile: File,
    outputFiles: List<File>,
    usePreciseJavaTracking: Boolean,
    modulesApiHistory: ModulesApiHistory,
    allKotlinExtensions: List<String>,
    classpathChanges: ClasspathChanges,
    withAbiSnapshot: Boolean,
    usePreciseCompilationResultsBackup: Boolean,
): IncrementalJvmCompilerRunner {
    return IncrementalJvmCompilerRunner(
        workingDir,
        reporter,
        buildHistoryFile = buildHistoryFile,
        outputDirs = outputFiles,
        usePreciseJavaTracking = usePreciseJavaTracking,
        modulesApiHistory = modulesApiHistory,
        kotlinSourceFilesExtensions = allKotlinExtensions,
        classpathChanges = classpathChanges,
        withAbiSnapshot = withAbiSnapshot,
        preciseCompilationResultsBackup = usePreciseCompilationResultsBackup,
    )
}
