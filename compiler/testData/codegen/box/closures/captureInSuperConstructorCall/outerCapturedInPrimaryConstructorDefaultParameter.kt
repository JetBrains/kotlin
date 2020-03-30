// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND_FIR: JVM_IR

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String = { x.toString() }
    )
}

fun box() = Outer("OK").Inner().fn()