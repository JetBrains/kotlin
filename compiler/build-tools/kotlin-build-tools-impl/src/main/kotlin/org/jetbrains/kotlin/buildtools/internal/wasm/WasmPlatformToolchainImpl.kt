/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.wasm

import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmKlibCompilationOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmLinkingOperationImpl
import java.nio.file.Path

internal class WasmPlatformToolchainImpl(private val compilerVersion: String) : WasmPlatformToolchain {
    override fun wasmLinkingOperationBuilder(klib: Path, destination: Path): WasmLinkingOperation.Builder =
        WasmLinkingOperationImpl(klib, destination)

    override fun wasmKlibCompilationOperationBuilder(sources: List<Path>, destination: Path): WasmKlibCompilationOperation.Builder =
        WasmKlibCompilationOperationImpl(
            sources,
            destination,
            compilerVersion = compilerVersion
        )
}
