// IGNORE_BACKEND: WASM
// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME

// FILE: 1.kt
package test

inline fun foo(vararg strs: Char) = strs.concatToString()

// FILE: 2.kt
import test.*

fun box(): String {
    return charArrayOf('O', 'K').let(::foo)
}

