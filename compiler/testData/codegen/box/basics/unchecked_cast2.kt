/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_1_9
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0 does not know this option
// KT-66084
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// KT-66085
// IGNORE_BACKEND: WASM
// WITH_STDLIB
import kotlin.test.*

fun box(): String {
    try {
        val x = cast<String>(Any())
        return "FAIL: ${x.length}"
    } catch (e: ClassCastException) {
        return "OK"
    }
}

fun <T> cast(x: Any?) = x as T