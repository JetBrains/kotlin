/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.wasm

import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmLinkingOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmLinkingOperationImpl
import org.jetbrains.kotlin.buildtools.internal.wasm.operations.WasmKlibCompilationOperationImpl
import java.nio.file.Path

internal class WasmPlatformToolchainImpl : WasmPlatformToolchain {
    override fun wasmLinkingOperationBuilder(sources: Path, destinationDirectory: Path): WasmLinkingOperation.Builder =
        WasmLinkingOperationImpl(sources, destinationDirectory)

    override fun wasmKlibCompilationOperationBuilder(sources: List<Path>, destinationKlib: Path): WasmKlibCompilationOperation.Builder =
        WasmKlibCompilationOperationImpl(sources, destinationKlib)
}
