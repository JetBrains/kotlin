// KT-90199
// Non-local destructuring declarations are error properties, which cannot be restored
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

val (c, d) = X(2, "")

val e = 3
