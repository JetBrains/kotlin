/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

abstract class WasmPlatform(platformName: String) : SimplePlatform(platformName), PotentiallyWebPlatform {
    override val oldFashionedDescription: String
        get() = "Wasm "

    override val isWeb: Boolean
        get() = true
}

fun TargetPlatform?.isWasm(): Boolean = this?.singleOrNull() is WasmPlatform