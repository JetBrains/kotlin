class A {
    private fun foo() = "OK"

    fun bar() = this.(::foo)()
}

fun box() = A().bar()
