// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6, WASM, NATIVE
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ISSUE: KT-57858

// FILE: Main.kt

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.internal.PlatformDependent

interface I {
    @PlatformDependent
    fun f(): String = "FAIL"
}

class C : I {
    fun f() = "OK"
}

fun box() = C().f()