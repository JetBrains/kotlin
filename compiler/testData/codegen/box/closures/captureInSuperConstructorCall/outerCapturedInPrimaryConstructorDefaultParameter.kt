// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String = { x.toString() }
    )
}

fun box() = Outer("OK").Inner().fn()