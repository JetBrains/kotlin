// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtDestructuringDeclaration

data class X(val a: Int, val b: Int)

fun main(x: X) {
    <expr>val (a, b) = x</expr>
}