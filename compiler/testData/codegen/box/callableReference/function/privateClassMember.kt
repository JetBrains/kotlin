class A {
    private fun foo() = "OK"

    fun bar() = (A::foo).let { it(this) }
}

fun box() = A().bar()
