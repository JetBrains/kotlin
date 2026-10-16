// KT-74654
// KT-90199
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// Top-level destructuring entries are not indexed, so they cannot be restored by their callable id (KT-90376)
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
annotation class Ann(val s: String)

const val x = "str"

data class X(val a: Int, val b: Int)

val (@Ann(x) <caret>a, b) = X(1, 2)
