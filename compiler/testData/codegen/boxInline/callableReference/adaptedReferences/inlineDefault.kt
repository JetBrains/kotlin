// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

// FILE: 1.kt
package test

inline fun foo(mkString: () -> String): String =
        mkString()

inline fun bar (xs: CharArray = charArrayOf('O','K')) =
        String(xs)

// FILE: 2.kt
import test.*

fun box(): String = foo(::bar)
