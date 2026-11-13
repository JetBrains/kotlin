/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.konan

import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.api.konan.operations.NativeKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.internal.konan.operations.NativeKlibCompilationOperationImpl
import java.nio.file.Path

internal class NativePlatformToolchainImpl(private val compilerVersion: String) : NativePlatformToolchain {
    override fun nativeKlibCompilationOperationBuilder(
        sources: List<Path>,
        destination: Path,
    ): NativeKlibCompilationOperation.Builder =
        NativeKlibCompilationOperationImpl(
            sources,
            destination,
            compilerVersion = compilerVersion,
        )
}
