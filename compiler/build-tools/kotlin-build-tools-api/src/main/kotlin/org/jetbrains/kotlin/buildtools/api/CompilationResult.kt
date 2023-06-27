/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * Represents the result of a compilation process.
 *
 * This enum is used as the return value of compilation methods to indicate the status of the compilation process.
 */
public enum class CompilationResult {
    /**
     * Represents a successful compilation state.
     */
    COMPILATION_SUCCESS,

    /**
     * Represents an error that occurs during compilation of code.
     * Usage:
     * The object of this class should be used when an error occurs during the compilation of code. It can be used to
     * identify and handle such errors during runtime.
     */
    COMPILATION_ERROR,

    /**
     * Represents a compilation error caused by running out of memory.
     *
     * This error is thrown when the compilation process has run out of memory and was unable to complete. This can occur when
     * the compilation task requires more memory than is available to the operating system or allocated to the process.
     *
     * This error should be handled by allocating more memory to the process or optimizing the compilation task to require
     * less memory.
     */
    COMPILATION_OOM_ERROR,

    /**
     * Represents an internal error that occurs within the compiler.
     *
     * This error should be [reported](https://kotl.in/issue) to the compiler developers for investigation and resolution.
     */
    COMPILER_INTERNAL_ERROR,
}