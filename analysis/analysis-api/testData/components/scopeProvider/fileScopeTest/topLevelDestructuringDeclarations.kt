// KT-90199
// Top-level destructuring entries are not indexed, so they cannot be restored by their callable id (KT-90376)
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

val (c, d) = X(2, "")

val e = 3
