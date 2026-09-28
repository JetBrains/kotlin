// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WITH_STDLIB
// TODO: Reified generics required some design to unify behavior across all backends
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.4.20
// KT-49422: Fixed in 2.5.0-Beta2
// FILE: 1.kt
package test

inline fun <reified T, reified R>T.castTo(): R = this as R

// FILE: 2.kt

import test.*

fun case1(): Int =
        null.castTo<Int?, Int>()

fun box(): String {
    failNPE { case1(); return "Fail" }
    return "OK"
}

inline fun failNPE(s: () -> Unit) {
    try {
        s()
    }
    catch (e: NullPointerException) {
        // OK
    }
}
