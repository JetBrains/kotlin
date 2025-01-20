/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.arguments.WasmTarget

public interface WasmLinkerArguments : BaseToolArguments {
    public class WasmLinkerArgument<V> internal constructor(public val id: String)

    public operator fun <V> get(key: WasmLinkerArgument<V>): V?

    public operator fun <V> set(key: WasmLinkerArgument<V>, value: V)

    public companion object {
        @JvmField
        public val WASM_TARGET: WasmLinkerArgument<WasmTarget> = WasmLinkerArgument("WASM_TARGET")

        @JvmField
        public val INCLUDE_DEBUG_INFORMATION: WasmLinkerArgument<Boolean> = WasmLinkerArgument("INCLUDE_DEBUG_INFORMATION") // -Xwasm-debug-info

        // ... the set of Wasm compiler arguments is currently merged with the set of Wasm linker arguments.
        // also they're mixed with K/JS arguments.
        // also there are a lot of experimental arguments
        // We should cooperate with K/Wasm to define the actual arguments for K/Wasm
    }
}