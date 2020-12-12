// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// WITH_RUNTIME

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