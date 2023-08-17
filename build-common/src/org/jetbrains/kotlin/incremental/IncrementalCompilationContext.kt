/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.DoNothingICReporter
import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.incremental.storage.FileToAbsolutePathConverter
import org.jetbrains.kotlin.incremental.storage.FileToPathConverter

class IncrementalCompilationContext(
    // The root directories of source files and class files are different, so we may need different `FileToPathConverter`s
    val pathConverterForSourceFiles: FileToPathConverter = FileToAbsolutePathConverter,
    val pathConverterForOutputFiles: FileToPathConverter = FileToAbsolutePathConverter,
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
    @Deprecated("This constructor is scheduled to be removed. KSP is using it")
    constructor(
        pathConverter: FileToPathConverter,
        storeFullFqNamesInLookupCache: Boolean = false,
        transaction: CompilationTransaction = NonRecoverableCompilationTransaction(),
        reporter: ICReporter = DoNothingICReporter,
        trackChangesInLookupCache: Boolean = false,
        keepIncrementalCompilationCachesInMemory: Boolean = false,
    ) : this(
        pathConverter,
        pathConverter,
        storeFullFqNamesInLookupCache,
        transaction,
        reporter,
        trackChangesInLookupCache,
        keepIncrementalCompilationCachesInMemory
    )

    // FIXME: Remove `pathConverter` and require its users to decide whether to use `pathConverterForSourceFiles` or
    // `pathConverterForClassFiles`
    val pathConverter = pathConverterForSourceFiles
}