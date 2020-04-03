// TODO: Reified generics required some design to unify behavior across all backends
// IGNORE_BACKEND: JS, JS_IR
// FILE: 1.kt
// WITH_RUNTIME
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
