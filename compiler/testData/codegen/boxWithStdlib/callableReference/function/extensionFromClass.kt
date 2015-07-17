class A {
    fun result() = (::foo)(this, "OK")
}

fun A.foo(x: String) = x

fun box() = A().result()
