/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.js

import org.jetbrains.kotlin.buildtools.api.WasmBinaryLinkingOperation
import org.jetbrains.kotlin.buildtools.api.WasmKlibCompilationOperation
import java.nio.file.Path

public interface WasmPlatformToolchain {
    /**
     * Creates a self-contained operation descriptor to be executed by [org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain.executeOperation]
     *
     * Basically, converts sources into klib files.
     */
    public fun createKlibCompilationOperation(kotlinSources: List<Path>, destinationDirectory: Path): WasmKlibCompilationOperation

    /**
     * Creates a self-contained operation descriptor to be executed by [org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain.executeOperation]
     *
     * Basically, converts a set of klib files into binaries, like a set of .wasm files, .js files (for wasm-js), source maps, etc...
     */
    public fun createBinaryLinkingOperation(klibPaths: List<Path>, destinationDirectory: Path): WasmBinaryLinkingOperation
}