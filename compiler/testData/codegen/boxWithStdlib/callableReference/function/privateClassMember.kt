class A {
    private fun foo() = "OK"

    fun bar() = (::foo)(this)
}

fun box() = A().bar()
