/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.compilation.CompilationOptions
import org.jetbrains.kotlin.buildtools.api.compilation.CompilationResult
import org.jetbrains.kotlin.buildtools.api.compilation.CompilationStrategySettings

/**
 * A facade for invoking compilation in Kotlin compiler. It allows to use compiler in different modes.
 * @see CompilationStrategySettings
 * @see CompilationOptions
 */
@ExperimentalBuildToolsApi
interface CompilationService {
    /**
     * Compiles the given source code using the specified compilation strategy settings,
     * arguments, and compilation options.
     *
     * @param compilationStrategySettings The settings for the compilation strategy to use.
     * @param arguments The arguments to pass to the compiler.
     * @param compilationOptions The options to use when compiling the code.
     * @return A [CompilationResult] that represents the result of the compilation process.
     */
    fun compile(
        compilationStrategySettings: CompilationStrategySettings,
        arguments: List<String>,
        compilationOptions: CompilationOptions,
    ): CompilationResult

    @ExperimentalBuildToolsApi
    companion object {
        /**
         * Loads the implementation of the [CompilationService] interface from the given class loader.
         *
         * @param classLoader The class loader to use for loading the implementation.
         * @return An instance of the [CompilationService] interface.
         */
        @JvmStatic
        fun loadImplementation(classLoader: ClassLoader) = loadImplementation(CompilationService::class, classLoader)
    }
}