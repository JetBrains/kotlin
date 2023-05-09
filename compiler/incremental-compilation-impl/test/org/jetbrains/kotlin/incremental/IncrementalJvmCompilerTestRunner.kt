/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.utils.TestBuildReporter
import org.jetbrains.kotlin.incremental.utils.TestLookupTracker
import java.io.File

class IncrementalJvmCompilerTestRunner(
    workingDir: File,
    val testReporter: TestBuildReporter,
    usePreciseJavaTracking: Boolean,
    buildHistoryFile: File,
    outputDirs: Collection<File>?,
    modulesApiHistory: ModulesApiHistory,
    override val kotlinSourceFilesExtensions: List<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    classpathChanges: ClasspathChanges,
    withAbiSnapshot: Boolean = false,
    val testLookupTracker: TestLookupTracker
) : IncrementalJvmCompilerRunner(
    workingDir,
    testReporter,
    usePreciseJavaTracking,
    buildHistoryFile,
    outputDirs,
    modulesApiHistory,
    kotlinSourceFilesExtensions,
    classpathChanges,
    withAbiSnapshot
) {
    override fun createCacheManager(icContext: IncrementalCompilationContext, args: K2JVMCompilerArguments): IncrementalJvmCachesManager =
        object : IncrementalJvmCachesManager(
            icContext, args.destination?.let { File(it) }, cacheDirectory
        ) {
            override fun close() {
                val platformCachesDump = this.platformCache.dump() +
                        "\n=============\n" +
                        this.inputsCache.dump().replace("rebuild-out", "out")

                testLookupTracker.lookups.mapTo(testLookupTracker.savedLookups) { LookupSymbol(it.name, it.scopeFqName) }
                this.lookupCache.forceGC()
                val lookupsDump = this.lookupCache.dump(testLookupTracker.savedLookups)

                testReporter.reportCachesDump("$platformCachesDump\n=============\n$lookupsDump")
                super.close()
            }
        }

    override fun getLookupTrackerDelegate() = testLookupTracker

}