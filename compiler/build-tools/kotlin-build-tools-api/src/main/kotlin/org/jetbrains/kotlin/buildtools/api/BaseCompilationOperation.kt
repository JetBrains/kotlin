/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker

@ExperimentalBuildToolsApi
public interface BaseCompilationOperation : BuildOperation<CompilationResult> {

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * An option for configuring a [BaseCompilationOperation].
     *
     * @see get
     * @see set
     * @see BaseCompilationOperation.Companion
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    /**
     * A builder for configuring a [BaseCompilationOperation].
     *
     * @since 2.4.0
     */
    public interface Builder : BuildOperation.Builder {
        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         *
         * @since 2.4.0
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         * @since 2.4.0
         */
        public operator fun <V> set(key: Option<V>, value: V)
    }

    public companion object {
        /**
         * Adds a tracker that will be informed whenever the compiler makes lookups for references.
         */
        @JvmField
        public val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = Option("LOOKUP_TRACKER", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls at which logging level to display the command line arguments passed to the compiler.
         *
         * Defaults to [CompilerArgumentsLogLevel.DEBUG].
         */
        @JvmField
        public val COMPILER_ARGUMENTS_LOG_LEVEL: Option<CompilerArgumentsLogLevel> = Option("COMPILER_ARGUMENTS_LOG_LEVEL", KotlinReleaseVersion(2, 3, 0))

        /**
         * Enables the Compiler Reference Index generation during the compilation.
         */
        @JvmField
        public val GENERATE_COMPILER_REF_INDEX: Option<Boolean> = Option("GENERATE_COMPILER_REF_INDEX", KotlinReleaseVersion(2, 3, 20))

        /**
         * Transform compiler diagnostics into formatted strings for output.
         *
         * If no specific renderer is provided, the system defaults to a standard format:
         * file://<path>:<line>:<column> <message>
         *
         * Example Output:
         * file:///path/to/File.kt:10:5 Unresolved reference: foo
         *
         * @see CompilerMessageRenderer
         */
        @JvmField
        public val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> = Option("COMPILER_MESSAGE_RENDERER", KotlinReleaseVersion(2, 4, 0))
    }
}
