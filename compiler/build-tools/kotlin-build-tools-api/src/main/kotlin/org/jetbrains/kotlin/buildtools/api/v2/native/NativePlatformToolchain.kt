/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.native

import org.jetbrains.kotlin.buildtools.api.v2.native.operations.NativeBinaryLinkingOperation
import org.jetbrains.kotlin.buildtools.api.v2.native.operations.NativeKlibCompilationOperation
import java.nio.file.Path

public interface NativePlatformToolchain {
    /**
     * Creates a self-contained operation descriptor to be executed by [org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain.executeOperation]
     *
     * Basically, converts sources into klib files.
     */
    public fun createKlibCompilationOperation(kotlinSources: List<Path>, destinationDirectory: Path): NativeKlibCompilationOperation

    /**
     * Creates a self-contained operation descriptor to be executed by [org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain.executeOperation]
     *
     * Basically, converts a set of klib files into binaries, like debug symbols, .exe, .framework, .solib, etc...
     */
    public fun createBinaryLinkingOperation(klibPaths: List<Path>, destinationDirectory: Path): NativeBinaryLinkingOperation
}