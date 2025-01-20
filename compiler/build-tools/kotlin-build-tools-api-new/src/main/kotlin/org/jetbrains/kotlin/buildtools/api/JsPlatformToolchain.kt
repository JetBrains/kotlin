/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

public interface JsPlatformToolchain {
    /**
     * Creates a self-contained operation descriptor to be executed by [KotlinToolchain.executeOperation]
     *
     * Basically, converts sources into klib files.
     */
    public fun makeKlibCompilationOperation(): JsKlibCompilationOperation

    /**
     * Creates a self-contained operation descriptor to be executed by [KotlinToolchain.executeOperation]
     *
     * Basically, converts a set of klib files into binaries, like a set of .js files, source maps, etc...
     */
    public fun makeBinaryLinkingOperation(): JsBinaryLinkingOperation
}