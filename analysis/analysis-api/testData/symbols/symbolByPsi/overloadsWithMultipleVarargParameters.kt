// KT-91580
// The second function is invalid, as it has two `vararg` parameters
package test

fun foo(vararg x: Int, y: IntArray) {}

fun foo(vararg x: Int, vararg y: Int) {}
