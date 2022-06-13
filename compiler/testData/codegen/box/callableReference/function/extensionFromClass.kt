class A {
    fun result() = (A::foo).let { it(this, "OK") }
}

fun A.foo(x: String) = x

fun box() = A().result()
