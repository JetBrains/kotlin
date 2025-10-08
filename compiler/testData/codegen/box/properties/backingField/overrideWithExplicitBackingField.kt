// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

interface Base {
    val a : Any
        get() = "not OK"
}

class Derived : Base {
    final override val a: Any
        field: String = "OK"

    fun usage(): String {
        return acceptString(a)
    }
}

fun acceptString(a: String): String {
    return a
}

fun box(): String {
    return Derived().usage()
}
