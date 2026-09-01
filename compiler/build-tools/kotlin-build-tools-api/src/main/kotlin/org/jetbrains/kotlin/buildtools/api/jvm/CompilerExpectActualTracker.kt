/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A tracker that will be informed whenever the compiler matches an `expect` declaration with its `actual`
 * counterpart.
 *
 * The two declarations live in different source files, and editing either one can invalidate the other, so both
 * sides have to be recompiled together.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerExpectActualTracker {
    /**
     * A callback that will be invoked when an `expect` declaration is matched with a real `actual` declaration.
     *
     * @param expectedFilePath the source file containing the `expect` declaration
     * @param actualFilePath the source file containing the matching `actual` declaration
     */
    public fun report(expectedFilePath: String, actualFilePath: String)

    /**
     * A callback that will be invoked when an `expect` declaration has no `actual` counterpart in the sources and
     * the compiler substitutes a generated placeholder for it.
     *
     * There is no second file to pair [expectedFilePath] with in this case, so a build system should not record a
     * dependency for it.
     *
     * @param expectedFilePath the source file containing the `expect` declaration
     */
    public fun reportExpectOfLenientStub(expectedFilePath: String)
}
