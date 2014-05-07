class A {
    fun result() = this.(::foo)("OK")
}

fun A.foo(x: String) = x

fun box() = A().result()
