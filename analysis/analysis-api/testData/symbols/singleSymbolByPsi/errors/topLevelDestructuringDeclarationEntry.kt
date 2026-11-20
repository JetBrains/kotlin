// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// Top-level destructuring entries are not indexed, so they cannot be restored by their callable id (KT-90376)
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

data class X(val a: Int, val b: Int)

val (<expr>a</expr>, b) = X(1, 2)
