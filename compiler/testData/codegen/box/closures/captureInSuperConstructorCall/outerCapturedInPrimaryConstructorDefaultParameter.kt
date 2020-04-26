// IGNORE_BACKEND: JS_IR

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String = { x.toString() }
    )
}

fun box() = Outer("OK").Inner().fn()


// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: FAILS_IN_JS_IR
