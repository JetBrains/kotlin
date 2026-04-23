/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.wasm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.getToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Allows creating operations that can be used for performing Kotlin/JS compilations.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Obtain an instance of this interface from [org.jetbrains.kotlin.buildtools.api.KotlinToolchains.wasm].
 *
 * @since 2.4.20
 */
@ExperimentalBuildToolsApi
public interface WasmPlatformToolchain : KotlinToolchains.Toolchain {
    /**
     * Creates a builder for an operation for producing the final output from a klib file
     *
     * @param klib the input klib file or directory that will be used for linking
     * @param destination where to put the output of the compilation
     */
    public fun wasmLinkingOperationBuilder(klib: Path, destination: Path): WasmLinkingOperation.Builder

    /**
     * Creates a builder for an operation for compiling Kotlin sources into a klib.
     *
     * @param sources all sources of the compilation unit
     * @param destination output klib file or directory
     *
     */
    public fun wasmKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): WasmKlibCompilationOperation.Builder

    public companion object {
        /**
         * Gets a [WasmPlatformToolchain] instance from [KotlinToolchains].
         *
         * Equivalent to `kotlinToolchains.getToolchain<WasmPlatformToolchain>()`
         */
        @JvmStatic
        @get:JvmName("from")
        public inline val KotlinToolchains.wasm: WasmPlatformToolchain get() = getToolchain<WasmPlatformToolchain>()
    }
}

/**
 * Convenience function for creating a [WasmLinkingOperation] with options configured by [builderAction].
 *
 * @return an immutable `WasmCompilationOperation`.
 * @see WasmPlatformToolchain.wasmLinkingOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun WasmPlatformToolchain.wasmLinkingOperation(
    sources: Path,
    destinationDirectory: Path,
    builderAction: WasmLinkingOperation.Builder.() -> Unit = {},
): WasmLinkingOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return wasmLinkingOperationBuilder(sources, destinationDirectory).apply(builderAction).build()
}

/**
 * Convenience function for creating a [WasmKlibCompilationOperation] with options configured by [builderAction].
 *
 * @return an immutable `JsKlibCompilationOperation`.
 * @see WasmPlatformToolchain.wasmKlibCompilationOperationBuilder
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalBuildToolsApi
public inline fun WasmPlatformToolchain.wasmKlibCompilationOperation(
    sources: List<Path>,
    destinationKlib: Path,
    builderAction: WasmKlibCompilationOperation.Builder.() -> Unit = {},
): WasmKlibCompilationOperation {
    contract {
        callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE)
    }
    return wasmKlibCompilationOperationBuilder(sources, destinationKlib).apply(builderAction).build()
}
