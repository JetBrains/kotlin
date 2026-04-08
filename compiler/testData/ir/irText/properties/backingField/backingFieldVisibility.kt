
// IGNORE_BACKEND_KLIB: JS_IR

class A {
    val a: Number
        field = 1

    val c = 1
    val d = c + 2

    fun rest() {
        val aI = A().a + 10
    }
}
