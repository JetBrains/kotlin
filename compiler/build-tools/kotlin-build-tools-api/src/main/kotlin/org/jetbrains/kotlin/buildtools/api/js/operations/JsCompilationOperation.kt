/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js.operations

import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker

/**
 * Compiles Kotlin code targeting JVM platform and using specified options.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JsPlatformToolchain.createJsCompilationOperation].
 *
 * An example of the basic usage is:
 *  ```
 *   val toolchain = KotlinToolchains.loadImplementation(ClassLoader.getSystemClassLoader())
 *   val operation = toolchain.jvm.createJvmCompilationOperation(listOf(Path("/path/foo.kt")), Path("/path/to/outputDirectory"))
 *   operation.compilerArguments[CommonCompilerArguments.LANGUAGE_VERSION] = KotlinVersion.V2_0
 *   toolchain.createBuildSession().use { it.executeOperation(operation, toolchain.createDaemonExecutionPolicy()) }
 *  ```
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface JsCompilationOperation : BuildOperation<CompilationResult> {
    /**
     * Base class for [JsCompilationOperation] options.
     *
     * @see get
     * @see set
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> set(key: Option<V>, value: V)

    /**
     * Kotlin compiler configurable options for JVM platform.
     */
    public val compilerArguments: JvmCompilerArguments

    public companion object {
        /**
         * Adds a tracker that will be informed whenever the compiler makes lookups for references.
         */
        @JvmField
        public val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER")

        /**
         * An array of additional Kotlin script extensions (on top of the default `.kt` and `.kts`).
         */
        @JvmField
        public val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = Option("KOTLINSCRIPT_EXTENSIONS")

        /**
         * Controls at which logging level to display the command line arguments passed to the compiler.
         *
         * Defaults to [CompilerArgumentsLogLevel.DEBUG].
         */
        @JvmField
        public val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> = Option("COMPILER_ARGUMENTS_LOG_LEVEL")
    }

    public enum class CompilerArgumentsLogLevel {
        ERROR,
        WARNING,
        INFO,
        DEBUG;
    }
}