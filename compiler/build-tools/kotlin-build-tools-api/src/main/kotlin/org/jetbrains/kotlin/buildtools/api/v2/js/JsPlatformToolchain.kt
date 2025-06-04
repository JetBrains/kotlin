/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.js

import org.jetbrains.kotlin.buildtools.api.v2.js.operations.JsBinaryLinkingOperation
import org.jetbrains.kotlin.buildtools.api.v2.js.operations.JsKlibCompilationOperation
import java.nio.file.Path

public interface JsPlatformToolchain {
    /**
     * Creates a self-contained operation descriptor to be executed by [org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain.executeOperation]
     *
     * Basically, converts sources into klib files.
     */
    public fun createKlibCompilationOperation(kotlinSources: List<Path>, destinationDirectory: Path): JsKlibCompilationOperation

    /**
     * Creates a self-contained operation descriptor to be executed by [org.jetbrains.kotlin.buildtools.api.v2.KotlinToolchain.executeOperation]
     *
     * Basically, converts a set of klib files into binaries, like a set of .js files, source maps, etc...
     */
    public fun createBinaryLinkingOperation(klibPaths: List<Path>, destinationDirectory: Path): JsBinaryLinkingOperation
}