// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry

data class X(val a: Int, val b: Int)

fun main(x: X) {
    var (<expr>a</expr>, b) = x
}