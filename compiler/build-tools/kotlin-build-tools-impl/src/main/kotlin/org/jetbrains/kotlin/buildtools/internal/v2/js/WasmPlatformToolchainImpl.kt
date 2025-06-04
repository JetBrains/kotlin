/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.js

import org.jetbrains.kotlin.buildtools.api.WasmBinaryLinkingOperation
import org.jetbrains.kotlin.buildtools.api.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.v2.js.WasmPlatformToolchain
import java.nio.file.Path

class WasmPlatformToolchainImpl: WasmPlatformToolchain {
    override fun createKlibCompilationOperation(
        kotlinSources: List<Path>,
        destinationDirectory: Path,
    ): WasmKlibCompilationOperation {
        TODO("Not yet implemented")
    }

    override fun createBinaryLinkingOperation(
        klibPaths: List<Path>,
        destinationDirectory: Path,
    ): WasmBinaryLinkingOperation {
        TODO("Not yet implemented")
    }

}
