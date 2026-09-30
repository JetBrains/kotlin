// KT-74654
// KT-89821: annotations on destructuring entries have no FIR yet
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtDestructuringDeclarationEntry
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
annotation class Ann(val s: String)

const val x = "str"

data class X(val a: Int, val b: Int)

val (@Ann(x) <caret>a, b) = X(1, 2)
