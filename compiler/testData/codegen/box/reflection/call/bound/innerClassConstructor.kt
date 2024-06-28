// TARGET_BACKEND: JVM
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
