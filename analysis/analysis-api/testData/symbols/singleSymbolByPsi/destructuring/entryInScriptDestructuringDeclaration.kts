// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

data class X(val a: Int, val b: Int)

val (<expr>c</expr>, d) = X(1, 2)
