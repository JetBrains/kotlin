// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6, WASM
// K2 issue: KT-64801

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String
    ) {
        constructor(
            unused: Int,
            fn: () -> String = { x.toString() }
        ) : this(fn)
    }
}

fun box() = Outer("OK").Inner(1).fn()
