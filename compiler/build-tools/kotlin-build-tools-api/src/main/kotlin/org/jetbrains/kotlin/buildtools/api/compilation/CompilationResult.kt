/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * An enumeration of possible results of a compilation process.
 */
@ExperimentalBuildToolsApi
enum class CompilationResult {
    /**
     * Indicates that the compilation completed successfully.
     */
    COMPILATION_SUCCESS,

    /**
     * Indicates that the compilation failed due to an error in the source code.
     */
    COMPILATION_ERROR,

    /**
     * Indicates that the compilation failed due to an out-of-memory error.
     */
    COMPILATION_OOM_ERROR,

    /**
     * Indicates that the compilation failed due to an internal error in the compiler.
     */
    COMPILER_INTERNAL_ERROR,
}