/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js.operations

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.CancellableBuildOperation
import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import java.nio.file.Path

/**
 * Compiles Kotlin code targeting JS platform and using specified options into a KLib.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JsPlatformToolchain.jsKlibCompilationOperationBuilder].
 *
 * @since 2.4.0
 */
@ExperimentalBuildToolsApi
public interface JsKlibCompilationOperation : BaseCompilationOperation, CancellableBuildOperation<CompilationResult> {

    /**
     * All sources of the compilation unit.
     */
    public val sources: List<Path>

    /**
     * The directory where the resulting KLib file will be placed.
     */
    public val destination: Path

    @OptIn(ExperimentalCompilerArgument::class)
    public val compilerArguments: JsArguments

    /**
     * A builder for configuring and instantiating the [JsKlibCompilationOperation].
     */
    public interface Builder : BuildOperation.Builder {
        /**
         * All sources of the compilation unit.
         */
        public val sources: List<Path>

        /**
         * Output KLib file.
         */
        public val destination: Path

        /**
         * Kotlin compiler configurable options for KLib-based compilation.
         */
        @OptIn(ExperimentalCompilerArgument::class)
        public val compilerArguments: JsArguments.Builder

        /**
         * Creates the configuration object for history-based incremental compilation (IC) in JS projects.
         * TODO
         */
        public fun historyBasedIcConfigurationBuilder(
            workingDirectory: Path,
            sourcesChanges: SourcesChanges,
            modulesInformation: List<IncrementalModule>,
        ): JsHistoryBasedIncrementalCompilationConfiguration.Builder

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
         * Creates an immutable instance of [JsKlibCompilationOperation] based on the configuration of this builder.
         */
        public fun build(): JsKlibCompilationOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [JsKlibCompilationOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [JsKlibCompilationOperation].
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
    @Deprecated(
        "Build operations will become immutable in an upcoming release. " +
                "Obtain an instance of a mutable builder for the operation from the appropriate `Toolchain` instead."
    )
    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        public val INCREMENTAL_COMPILATION: Option<JsIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION")
    }
}
