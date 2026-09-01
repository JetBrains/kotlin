/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A tracker that will be informed which source files produced each file the compiler writes.
 *
 * A build system that recompiles incrementally needs this mapping to know which outputs to delete or replace when
 * a source file changes or is removed.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerFileMappingTracker {
    /**
     * A callback that will be invoked when the compiler writes an output file.
     *
     * @param sourceFilePaths the source files the output was produced from
     * @param outputFilePath the file that was written
     */
    public fun recordSourceFilesToOutputFileMapping(sourceFilePaths: Collection<String>, outputFilePath: String)

    /**
     * A callback that will be invoked when a source file is read while producing declarations for a compiler
     * plugin.
     *
     * @param sourceFilePath the source file that was read
     */
    public fun recordSourceReferencedByCompilerPlugin(sourceFilePath: String)

    /**
     * A callback that will be invoked when an output file is produced from declarations a compiler plugin
     * generated, rather than from source files on disk.
     *
     * @param outputFilePath the file that was written
     */
    public fun recordOutputFileGeneratedForPlugin(outputFilePath: String)

    /**
     * A callback that will be invoked when a compiler plugin generates a source file that is not present on disk.
     *
     * @param sourceFilePath the path reported for the generated source file
     */
    public fun recordSourceFileGeneratedForPlugin(sourceFilePath: String)
}
