/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.utils

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.incremental.IncrementalCompilationContext
import org.jetbrains.kotlin.incremental.IncrementalJvmCachesManager
import org.jetbrains.kotlin.incremental.LookupSymbol
import java.io.File

class IncrementalJvmCachesTestManager(
    icContext: IncrementalCompilationContext,
    args: K2JVMCompilerArguments,
    cacheDirectory: File,
    private val testLookupTracker: TestLookupTracker,
    private val testReporter: TestBuildReporter,
) : IncrementalJvmCachesManager(
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
