/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

abstract class WasmPlatform : SimplePlatform("Wasm") {
    override val oldFashionedDescription: String
        get() = "WebAssembly "
}

fun TargetPlatform?.isWasm(): Boolean = this?.singleOrNull() is WasmPlatform