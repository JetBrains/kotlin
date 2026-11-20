/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import java.io.File

/**
 * Runs the multi-module JS klib tests with klib snapshots instead of build history files. Like the Gradle transform, it snapshots each
 * dependency klib, and passes the snapshot files to the runner as [ClasspathChanges].
 */
abstract class AbstractIncrementalJsKlibMultiModuleWithKlibSnapshotsCompilerRunnerTest :
    AbstractIncrementalJsKlibMultiModuleCompilerRunnerTest() {

    override fun transformToDependency(moduleName: String, rawArtifact: File): File {
        TODO("KT-90559 step 3")
    }

    override fun makeForSingleModule(
        moduleCacheDir: File,
        sourceRoots: Iterable<File>,
        args: K2JSCompilerArguments,
        moduleBuildHistoryFile: File,
        messageCollector: MessageCollector,
        reporter: ICReporter,
        scopeExpansion: CompileScopeExpansionMode,
        modulesApiHistory: ModulesApiHistoryJs,
        providedChangedFiles: ChangedFiles?
    ) {
        TODO("KT-90559 step 3")
    }
}
