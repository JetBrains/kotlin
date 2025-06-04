/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.v2.native

import org.jetbrains.kotlin.buildtools.api.v2.native.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.api.v2.native.operations.NativeBinaryLinkingOperation
import org.jetbrains.kotlin.buildtools.api.v2.native.operations.NativeKlibCompilationOperation
import java.nio.file.Path

class NativePlatformToolchainImpl : NativePlatformToolchain{
    override fun createKlibCompilationOperation(
        kotlinSources: List<Path>,
        destinationDirectory: Path,
    ): NativeKlibCompilationOperation {
        TODO("Not yet implemented")
    }

    override fun createBinaryLinkingOperation(
        klibPaths: List<Path>,
        destinationDirectory: Path,
    ): NativeBinaryLinkingOperation {
        TODO("Not yet implemented")
    }

}
