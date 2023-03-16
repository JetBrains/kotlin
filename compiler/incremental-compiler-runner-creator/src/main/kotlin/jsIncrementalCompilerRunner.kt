/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import java.io.File

fun createIncrementalJsCompilerRunner(
    workingDir: File,
    reporter: BuildReporter,
    buildHistoryFile: File,
    scopeExpansion: CompileScopeExpansionMode,
    modulesApiHistory: ModulesApiHistory,
    withAbiSnapshot: Boolean,
    usePreciseCompilationResultsBackup: Boolean,
): IncrementalJsCompilerRunner {
    return IncrementalJsCompilerRunner(
        workingDir = workingDir,
        reporter = reporter,
        buildHistoryFile = buildHistoryFile,
        scopeExpansion = scopeExpansion,
        modulesApiHistory = modulesApiHistory,
        withAbiSnapshot = withAbiSnapshot,
        preciseCompilationResultsBackup = usePreciseCompilationResultsBackup,
    )
}