open class A<T> {
    private fun foo(x: T) = x
    internal inline fun callFoo(x: T): String {
        return if (x is Int) foo(x).toString() else ""
    }
}

fun box() = "OK"