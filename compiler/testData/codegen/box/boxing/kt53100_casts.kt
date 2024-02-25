/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// KT-54635: expected:<[OK]> but was:<[FAIL 1: 0]>
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// KT-66099: CompileError: WebAssembly.Module(): Compiling function #3415:"box" failed: Invalid types for ref.cast null: local.get of type f64 has to be in the same reference type hierarchy as (ref 686) @+237036
// IGNORE_BACKEND: WASM
// WITH_STDLIB

import kotlin.test.*

// Reproducer is copied from FloatingPointParser.unaryMinus()
inline fun <reified T> unaryMinus(value: T): T {
    return when (value) {
        is Float -> -value as T
        is Double -> -value as T
        else -> throw NumberFormatException()
    }
}

fun box(): String {
    val res1 = unaryMinus(0.0).toString()
    if (res1 != "-0.0") return "FAIL 1: $res1"

    val res2 = unaryMinus(0.0f).toString()
    if (res2 != "-0.0") return "FAIL 2: $res2"

    return "OK"
}
