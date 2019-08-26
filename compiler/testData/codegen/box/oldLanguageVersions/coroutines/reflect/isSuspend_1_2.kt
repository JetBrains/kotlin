// !LANGUAGE: -ReleaseCoroutines
// IGNORE_BACKEND: WASM
// WITH_COROUTINES
// WITH_REFLECT
// DONT_TARGET_EXACT_BACKEND: JS_IR

// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS

import helpers.*
import kotlin.coroutines.experimental.*

class A {
    fun noArgs() = "OK"
}

fun box(): String {
    if (A::noArgs.isSuspend) return "FAIL"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ experimental 
