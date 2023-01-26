/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter
import org.jetbrains.kotlin.incremental.storage.IncrementalFileToPathConverter
import java.io.File

private fun createDefaultPathConverter(rootProjectDir: File?) = IncrementalFileToPathConverter(rootProjectDir)

class IncrementalCompilationContext(
    val pathConverter: FileToPathConverter,
    val storeFullFqNamesInLookupCache: Boolean = false,
    val transaction: CompilationTransaction = NonRecoverableCompilationTransaction(),
    val reporter: ICReporter = DoNothingICReporter,
    /**
     * Controls whether changes in lookup cache should be tracked. Required for the classpath snapshots based IC approach
     */
    val trackChangesInLookupCache: Boolean = false,
    /**
     * Controls whether any changes should be propagated to FS until we decide that the compilation is successful or not
     *
     * Required for optimizing Gradle side outputs backup
     */
    val keepIncrementalCompilationCachesInMemory: Boolean = false,
) {
    constructor(
        rootProjectDir: File?,
        storeFullFqNamesInLookupCache: Boolean = false,
        transaction: CompilationTransaction = NonRecoverableCompilationTransaction(),
        reporter: ICReporter = DoNothingICReporter,
        trackChangesInLookupCache: Boolean = false,
        keepIncrementalCompilationCachesInMemory: Boolean = false,
    ) : this(
        createDefaultPathConverter(rootProjectDir),
        storeFullFqNamesInLookupCache,
        transaction,
        reporter,
        trackChangesInLookupCache,
        keepIncrementalCompilationCachesInMemory
    )
}