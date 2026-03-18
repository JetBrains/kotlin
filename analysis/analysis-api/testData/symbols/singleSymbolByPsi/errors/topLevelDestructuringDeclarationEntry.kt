// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// IGNORE_FE10

data class X(val a: Int, val b: Int)

val (<expr>a</expr>, b) = X(1, 2)
