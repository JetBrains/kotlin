open class A<T, S> {
    open fun foo(y: T, vararg x: S) {}
}

class B: A<Int, String>() {
    override fun foo(y: Int, vararg x: String) {}
}

fun box(): String {
    A<Any, Number>().foo("1", 1, 2, 3)
    B().foo(1, "", "1")
    return "OK"
}