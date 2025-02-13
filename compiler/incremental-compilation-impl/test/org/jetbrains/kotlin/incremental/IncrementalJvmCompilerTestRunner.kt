/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.utils.TestBuildReporter
import org.jetbrains.kotlin.incremental.utils.IncrementalJvmCachesTestManager
import org.jetbrains.kotlin.incremental.utils.TestLookupTracker
import java.io.File

class IncrementalJvmCompilerTestRunner(
    workingDir: File,
    val testReporter: TestBuildReporter,
    buildHistoryFile: File,
    outputDirs: Collection<File>?,
    modulesApiHistory: ModulesApiHistory,
    kotlinSourceFilesExtensions: Set<String>,
    icFeatures: IncrementalCompilationFeatures,
    val testLookupTracker: TestLookupTracker
) : BuildHistoryJvmICRunner(
    workingDir,
    testReporter,
    buildHistoryFile,
    outputDirs,
    modulesApiHistory,
    kotlinSourceFilesExtensions,
    icFeatures,
) {
    override fun createCacheManager(icContext: IncrementalCompilationContext, args: K2JVMCompilerArguments): IncrementalJvmCachesManager =
        IncrementalJvmCachesTestManager(
            icContext,
            args,
            cacheDirectory,
            testLookupTracker,
            testReporter,
        )

    override fun getLookupTrackerDelegate() = testLookupTracker
}
