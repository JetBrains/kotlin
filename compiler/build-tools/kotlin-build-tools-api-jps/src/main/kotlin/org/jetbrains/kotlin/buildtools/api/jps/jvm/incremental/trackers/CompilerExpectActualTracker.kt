/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi

/**
 * A tracker that will be informed which source files an `expect` declaration and its `actual` counterpart live in.
 *
 * The two files have to be recompiled together: whether they still match is checked while compiling, and neither
 * file records afterwards which file it was matched against.
 *
 * @since 2.5.0
 */
@InternalBuildToolsApi
public interface CompilerExpectActualTracker {
    /**
     * A callback that will be invoked when an `expect` declaration is matched with its `actual` counterpart.
     *
     * @param expectFilePath the source file containing the `expect` declaration
     * @param actualFilePath the source file containing the matching `actual` declaration
     */
    public fun report(expectFilePath: String, actualFilePath: String)

    /**
     * A callback that will be invoked when an `expect` declaration has no `actual` counterpart and the compiler
     * substitutes one that throws when called, as leniently compiling code without its platform implementations
     * asks for.
     *
     * Without it, adding the missing `actual` declaration later may not recompile the `expect` declaration, leaving
     * the substitute in place.
     *
     * @param expectFilePath the source file containing the `expect` declaration
     */
    public fun reportExpectOfLenientStub(expectFilePath: String)
}
