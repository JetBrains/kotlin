/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.nio.file.Path

public interface JsCompilerArguments : BaseKlibCompilerArguments {
    public class JsCompilerArgument<V> internal constructor(public val id: String)

    public operator fun <V> get(key: JsCompilerArgument<V>): V?

    public operator fun <V> set(key: JsCompilerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val LIBRARIES: JsCompilerArgument<List<Path>> = JsCompilerArgument("LIBRARIES")

        // ... the set of JS compiler arguments is currently merged with the set of JS linker arguments
        // also there are a lot of experimental arguments
        // We should cooperate with K/JS to define the actual arguments for K/JS
    }
}