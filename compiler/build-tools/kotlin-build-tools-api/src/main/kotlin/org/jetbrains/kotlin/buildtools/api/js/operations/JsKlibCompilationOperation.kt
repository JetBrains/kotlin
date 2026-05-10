/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js.operations

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JsArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Compiles Kotlin code targeting JS platform and using specified options into a klib.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [JsPlatformToolchain.jsKlibCompilationOperationBuilder].
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public interface JsKlibCompilationOperation : BaseCompilationOperation, CancellableBuildOperation<CompilationResult> {

    /**
     * All sources of the compilation unit.
     */
    public val sources: List<Path>

    /**
     * The directory where the resulting klib will be placed.
     */
    public val destination: Path

    @OptIn(ExperimentalCompilerArgument::class)
    public val compilerArguments: JsArguments

    /**
     * A builder for configuring and instantiating the [JsKlibCompilationOperation].
     */
    public interface Builder : BaseCompilationOperation.Builder {
        /**
         * All sources of the compilation unit.
         */
        public val sources: List<Path>

        /**
         * Output klib file.
         */
        public val destination: Path

        /**
         * Kotlin compiler configurable options for klib-based compilation.
         */
        @OptIn(ExperimentalCompilerArgument::class)
        public val compilerArguments: JsArguments.Builder

        /**
         * Creates the configuration object for history-based incremental compilation (IC) in JS projects.
         *
         * @param rootProjectDir the root directory of the project
         * @param workingDirectory the directory where the compiler will store the incremental compilation caches
         * @param sourcesChanges the changes in the source files, which can be unknown, to-be-calculated, or known
         * @param modulesInformation the information about modules layout in the project
         */
        public fun historyBasedIcConfigurationBuilder(
            rootProjectDir: Path,
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
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object {
        @JvmField
        public val INCREMENTAL_COMPILATION: Option<JsIncrementalCompilationConfiguration?> =
            Option("INCREMENTAL_COMPILATION", KotlinReleaseVersion(2, 4, 20))
    }
}


/**
 * Convenience function for creating a [JsKlibCompilationOperation] with options configured by [builderAction].
 *
 * @return an immutable `JsKlibCompilationOperation`.
 * @see JsPlatformToolchain.jsKlibCompilationOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun JsKlibCompilationOperation.Builder.historyBasedIcConfiguration(
    rootProjectDir: Path,
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    modulesInformation: List<IncrementalModule>,
    builderAction: JsHistoryBasedIncrementalCompilationConfiguration.Builder.() -> Unit = {},
): JsHistoryBasedIncrementalCompilationConfiguration {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return historyBasedIcConfigurationBuilder(rootProjectDir, workingDirectory, sourcesChanges, modulesInformation).apply(builderAction)
        .build()
}
