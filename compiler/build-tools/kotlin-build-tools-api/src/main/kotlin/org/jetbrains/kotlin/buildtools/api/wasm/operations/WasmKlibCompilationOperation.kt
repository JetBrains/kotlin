/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.wasm.operations

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmCompilerKlibArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.wasm.WasmIncrementalCompilationConfiguration
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Compiles Kotlin code targeting JS platform and using specified options into a klib.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [WasmPlatformToolchain.wasmKlibCompilationOperationBuilder].
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public interface WasmKlibCompilationOperation : BaseCompilationOperation, CancellableBuildOperation<CompilationResult> {

    /**
     * All sources of the compilation unit.
     */
    public val sources: List<Path>

    /**
     * The directory where the resulting klib will be placed.
     */
    public val destination: Path

    @OptIn(ExperimentalCompilerArgument::class)
    public val compilerArguments: WasmCompilerKlibArguments

    /**
     * A builder for configuring and instantiating the [WasmKlibCompilationOperation].
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
        public val compilerArguments: WasmCompilerKlibArguments.Builder

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
        ): WasmHistoryBasedIncrementalCompilationConfiguration.Builder

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
         * Creates an immutable instance of [WasmKlibCompilationOperation] based on the configuration of this builder.
         */
        public fun build(): WasmKlibCompilationOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [WasmKlibCompilationOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [WasmKlibCompilationOperation].
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    public companion object {
        @JvmField
        public val INCREMENTAL_COMPILATION: Option<WasmIncrementalCompilationConfiguration?> = Option("INCREMENTAL_COMPILATION")
    }
}


/**
 * Convenience function for creating a [WasmKlibCompilationOperation] with options configured by [builderAction].
 *
 * @return an immutable `WasmKlibCompilationOperation`.
 * @see WasmPlatformToolchain.wasmKlibCompilationOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun WasmKlibCompilationOperation.Builder.historyBasedIcConfiguration(
    rootProjectDir: Path,
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    modulesInformation: List<IncrementalModule>,
    builderAction: WasmHistoryBasedIncrementalCompilationConfiguration.Builder.() -> Unit = {},
): WasmHistoryBasedIncrementalCompilationConfiguration {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return historyBasedIcConfigurationBuilder(rootProjectDir, workingDirectory, sourcesChanges, modulesInformation).apply(builderAction)
        .build()
}
