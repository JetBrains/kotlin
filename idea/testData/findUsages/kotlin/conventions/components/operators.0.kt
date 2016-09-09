// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>x: Int, val y: Int) {
    fun f() {
        val (x, y) = this
    }
}

class B

operator fun B.plus(other: B): A = TODO()

fun f(b1: B, b2: B) {
    val (x, y) = b1 + b2
}
