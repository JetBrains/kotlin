// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt
inline fun test(
    block: () -> Int = {
        val o = object {
            lateinit var s: String
        }
        o.s.length
    }
) = block()

// MODULE: main(lib)
// DISABLE_IR_VISIBILITY_CHECKS: NATIVE, WASM_JS, WASM_WASI
// ^ UninitializedPropertyAccessException is internal on Native and Wasm
// FILE: main.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.test.*

fun box(): String {
    assertFailsWith<UninitializedPropertyAccessException> {
        test()
    }
    return "OK"
}