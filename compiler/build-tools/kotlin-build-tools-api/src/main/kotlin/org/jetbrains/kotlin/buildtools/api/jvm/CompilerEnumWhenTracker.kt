/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A tracker that will be informed whenever the compiler encounters a `when` expression over a Java enum.
 *
 * A `when` over an enum may be exhaustive, so adding or removing an entry can break code that used to compile. The
 * compiled output does not record which enum a `when` was written over, so it can only be observed at compile time.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerEnumWhenTracker {
    /**
     * A callback that will be invoked when a `when` expression over a Java enum is compiled.
     *
     * @param whenExpressionFilePath the source file containing the `when` expression
     * @param enumClassFqName fully qualified name of the enum class, with nested classes separated by `$`
     *   (for example, `com.example.Outer$Color`)
     */
    public fun report(whenExpressionFilePath: String, enumClassFqName: String)
}
