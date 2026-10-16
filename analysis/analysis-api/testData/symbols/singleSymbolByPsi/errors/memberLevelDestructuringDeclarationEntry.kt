// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry

data class X(val a: Int, val b: Int)

class B {
    val (<expr>a</expr>, b) = X(1, 2)
}
