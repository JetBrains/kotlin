// RESOLVE_SCRIPT
// MEMBER_NAME_FILTER: <destruct>
package util

annotation class Anno(val str: String)
const val constant = 1
data class MyPair<A>(val a: A, val b: Int)
val pair: @Anno(0 + constant) MyPair<@Anno(1 + constant) List<@Anno(2 + constant) Int>>

val (a, b) = pair