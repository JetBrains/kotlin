/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.wasm.operations

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.WasmArguments
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface WasmLinkingOperationProperties {
    /**
     * All klibs of the compilation unit.
     */
    public val klib: Path

    /**
     * Where to put the output of the compilation
     */
    public val destinationDirectory: Path

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: WasmLinkingOperation.Option<V>): V
}

/**
 * Compiles Kotlin code targeting Wasm platform and using specified options.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [WasmPlatformToolchain.wasmLinkingOperationBuilder].
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface WasmLinkingOperation : CancellableBuildOperation<CompilationResult>, WasmLinkingOperationProperties {

    @OptIn(ExperimentalCompilerArgument::class)
    public val compilerArguments: WasmArguments

    /**
     * A builder for configuring and instantiating the [WasmLinkingOperation].
     */
    public interface Builder : BuildOperation.Builder, WasmLinkingOperationProperties {
        /**
         * Kotlin compiler configurable options for Wasm platform.
         */
        @OptIn(ExperimentalCompilerArgument::class)
        public val compilerArguments: WasmArguments.Builder

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [WasmLinkingOperation] based on the configuration of this builder.
         */
        public fun build(): WasmLinkingOperation
    }

    /**
     * Returns a [Builder] initialized with the values of this [WasmLinkingOperation].
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [WasmLinkingOperation].
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    @Deprecated(
        "Build operations will become immutable in an upcoming release. " +
                "Obtain an instance of a mutable builder for the operation from the appropriate `Toolchain` instead."
    )
    public operator fun <V> set(key: Option<V>, value: V)
}
