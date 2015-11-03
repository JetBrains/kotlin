class Outer<E>(val x: E) {
    inner class Inner<F>(val y: F) {
        fun foo() = x.toString() + y.toString()
    }
}

object Test {
    fun foo(x: Outer<String>.Inner<Integer>) = x.foo()
}

fun box(): String {
    val result = JavaClass.test()
    if (result != "OK1") return "Fail: $result"
    return "OK"
}
