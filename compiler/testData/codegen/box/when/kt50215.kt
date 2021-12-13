class A {
    fun foo(msg: String = "OK") = msg
}
fun whoops(x: Any): String {
    return when (x) {
        is A -> x.foo()
        else -> throw AssertionError()
    }
}
fun box() = whoops(A())
