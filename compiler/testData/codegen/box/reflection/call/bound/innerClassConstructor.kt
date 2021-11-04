// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

class Outer(val x: String) {
    inner class Inner(val y: String) {
        fun foo() = x + y
    }
}

fun box(): String {
    val innerCtor = Outer("O")::Inner
    val inner = innerCtor.call("K")
    return inner.foo()
}
