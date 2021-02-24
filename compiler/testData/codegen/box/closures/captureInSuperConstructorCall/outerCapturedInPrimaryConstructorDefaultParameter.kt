// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: FAILS_IN_JS_IR
// IGNORE_BACKEND: JS_IR_ES6

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String = { x.toString() }
    )
}

fun box() = Outer("OK").Inner().fn()