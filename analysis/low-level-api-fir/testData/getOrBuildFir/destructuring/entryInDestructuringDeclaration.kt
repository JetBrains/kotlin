// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

data class X(val a: Int, val b: Int)

fun main(x: X) {
    val (<expr>a</expr>, b) = x
}