// IGNORE_BACKEND: JS_IR
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
