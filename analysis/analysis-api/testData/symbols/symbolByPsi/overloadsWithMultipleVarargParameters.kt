// KT-91580
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
// The second function is invalid, as it has two `vararg` parameters
package test

fun foo(vararg x: Int, y: IntArray) {}

fun foo(vararg x: Int, vararg y: Int) {}
