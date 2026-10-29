/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.cri.CompilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation.CompilerArgumentsLogLevel
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
         * Kotlin compiler configurable options.
         *
         * @since 2.4.20
         */
        public val compilerArguments: CommonCompilerArguments.Builder

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

        /**
         * Creates an immutable instance of [BaseCompilationOperation] based on the configuration of this builder.
         *
         * @since 2.4.20
         */
        public fun build(): BaseCompilationOperation

        /**
         * Creates a builder for the [CompilerRefIndexConfiguration] to set as [COMPILER_REF_INDEX].
         *
         * @param outputDirectory the directory the index files are written to
         * @throws IllegalStateException if the implementation is older than 2.5.20
         * @since 2.5.20
         */
        public fun compilerRefIndexConfigurationBuilder(outputDirectory: Path): CompilerRefIndexConfiguration.Builder {
            error("The compiler reference index configuration is supported from compiler version 2.5.20.")
        }
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
         *
         * The index goes to the [org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.DATA_PATH] directory under the working
         * directory of [org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration], so it takes
         * effect only with that configuration. When [COMPILER_REF_INDEX] is set to a configuration, this option is ignored.
         */
        @Deprecated("Use `COMPILER_REF_INDEX` instead")
        @JvmField
        public val GENERATE_COMPILER_REF_INDEX: Option<Boolean> = Option("GENERATE_COMPILER_REF_INDEX", KotlinReleaseVersion(2, 3, 20))

        /**
         * Enables the compiler reference index of the compilation and sets its output directory.
         *
         * Build the value with [compilerRefIndexConfiguration] or [Builder.compilerRefIndexConfigurationBuilder].
         * Takes precedence over the deprecated [GENERATE_COMPILER_REF_INDEX].
         *
         * @since 2.5.20
         */
        @JvmField
        public val COMPILER_REF_INDEX: Option<CompilerRefIndexConfiguration?> = Option("COMPILER_REF_INDEX", KotlinReleaseVersion(2, 5, 20))

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
         * @see CompilerMessageRendererWithDiagnosticId
         */
        @JvmField
        public val COMPILER_MESSAGE_RENDERER: Option<CompilerMessageRenderer> = Option("COMPILER_MESSAGE_RENDERER", KotlinReleaseVersion(2, 4, 0))
    }
}

/**
 * Creates a [CompilerRefIndexConfiguration] that writes to [outputDirectory], with the options set by [builderAction].
 *
 * @return an immutable `CompilerRefIndexConfiguration`.
 * @see BaseCompilationOperation.Builder.compilerRefIndexConfigurationBuilder
 * @since 2.5.20
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun BaseCompilationOperation.Builder.compilerRefIndexConfiguration(
    outputDirectory: Path,
    builderAction: CompilerRefIndexConfiguration.Builder.() -> Unit = {},
): CompilerRefIndexConfiguration {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return compilerRefIndexConfigurationBuilder(outputDirectory).apply(builderAction).build()
}
