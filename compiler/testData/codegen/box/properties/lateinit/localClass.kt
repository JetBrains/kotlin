// WITH_STDLIB

// DISABLE_IR_VISIBILITY_CHECKS: NATIVE, WASM
// ^ UninitializedPropertyAccessException is internal on Native and Wasm

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

import kotlin.UninitializedPropertyAccessException

fun box(): String {
    val o = object {
        lateinit var x: Any
    }
    try {
        if (o.x == null) return "fail 1"
        return "fail 2"
    } catch (t: UninitializedPropertyAccessException) {
        return "OK"
    }
}