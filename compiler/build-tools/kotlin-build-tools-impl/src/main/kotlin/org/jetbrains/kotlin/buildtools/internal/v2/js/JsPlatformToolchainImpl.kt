/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.js

import org.jetbrains.kotlin.buildtools.api.v2.js.operations.JsBinaryLinkingOperation
import org.jetbrains.kotlin.buildtools.api.v2.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.v2.js.JsPlatformToolchain
import java.nio.file.Path

class JsPlatformToolchainImpl : JsPlatformToolchain {
    override fun createKlibCompilationOperation(
        kotlinSources: List<Path>,
        destinationDirectory: Path,
    ): JsKlibCompilationOperation {
        TODO("Not yet implemented")
    }

    override fun createBinaryLinkingOperation(
        klibPaths: List<Path>,
        destinationDirectory: Path,
    ): JsBinaryLinkingOperation {
        TODO("Not yet implemented")
    }

}
