interface A<T> {
    fun foo(t: T): T = t
}

class B : A<String> {
    private fun foo() {}
}

fun box(): String = B().foo("OK")
