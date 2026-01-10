// FILE: lib.kt
class A {
    private inline fun f() = g()
    private fun g() = "OK"
    fun h() = { f() }
}

// FILE: main.kt
fun box() = A().h()()
