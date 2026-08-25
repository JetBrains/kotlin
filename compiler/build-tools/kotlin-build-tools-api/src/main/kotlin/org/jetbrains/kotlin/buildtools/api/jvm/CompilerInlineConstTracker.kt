/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * A tracker that will be informed whenever the compiler copies the value of a Java constant into the code it
 * produces.
 *
 * The value is written into the output rather than read at runtime, and the reference to the declaring class does
 * not survive, so the dependency can only be observed at compile time.
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public interface CompilerInlineConstTracker {
    /**
     * A callback that will be invoked when the value of a Java constant is copied into the compiled output.
     *
     * @param filePath the source file that reads the constant
     * @param owner fully qualified name of the Java class declaring the constant, with nested classes separated by
     *   `$` (for example, `com.example.Outer$Inner`)
     * @param name the name of the constant
     * @param constType the Kotlin type of the constant: one of `Byte`, `Short`, `Int`, `Long`, `Float`,
     *   `Double`, `Boolean`, `Char` or `String`
     */
    public fun report(filePath: String, owner: String, name: String, constType: String)
}
