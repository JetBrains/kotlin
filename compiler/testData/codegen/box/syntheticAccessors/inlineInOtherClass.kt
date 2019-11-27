// IGNORE_BACKEND_FIR: JVM_IR
class A {
    private inline fun f() = g()
    private fun g() = "OK"
    fun h() = { f() }
}

fun box() = A().h()()
