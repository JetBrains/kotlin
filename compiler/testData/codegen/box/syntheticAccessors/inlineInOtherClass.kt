class A {
    private inline fun f() = g()
    private fun g() = "OK"
    fun h() = { f() }
}

fun box() = A().h()()
