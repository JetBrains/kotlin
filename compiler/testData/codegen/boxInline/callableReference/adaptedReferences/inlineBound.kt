// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS

// FILE: 1.kt
package test

inline fun foo(x: () -> Unit): String {
    x()
    return "OK"
}

inline fun String.id(s: String = this, vararg xs: Int): String = s

// FILE: 2.kt
import test.*

fun box(): String = foo("Fail"::id)
