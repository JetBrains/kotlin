// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM
// K2 issue: KT-64801

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String = { x.toString() }
    )
}

fun box() = Outer("OK").Inner().fn()
