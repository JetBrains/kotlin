/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.psi.KtFile

/**
 * A source file to be compiled as a part of some [ChunkToCompile].
 */
internal class FileToCompile(val ktFile: KtFile, val firFile: FirFile)

/**
 * A set of files to be compiled together.
 *
 * @param kind A chunk kind.
 * @param mainFile The main file if a chunk is the [ChunkKind.MAIN] one, or `null` otherwise.
 * @param hasCodeFragments Whether [files] contain at least one code fragment.
 * @param attachPrecompiledBinaries Whether to attach compiled bytecode of the module instead of compiling the module files.
 * @param files Selected files that are either from the same module or should be compiled as they are from the same module.
 */
internal class ChunkToCompile(
    val kind: ChunkKind,
    val mainFile: KtFile?,
    val hasCodeFragments: Boolean,
    val attachPrecompiledBinaries: Boolean,
    val files: List<FileToCompile>
)

internal enum class ChunkKind {
    /** A chunk with the main file (a file for which compilation is requested). */
    MAIN,

    /** A chunk with the context of the main file. */
    CONTEXT,

    /**
     * A chunk with files required to be compiled prior to the main file compilation.
     */
    DEPENDENCY
}
