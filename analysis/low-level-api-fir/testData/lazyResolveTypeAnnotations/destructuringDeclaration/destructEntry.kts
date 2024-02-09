// RESOLVE_SCRIPT
// MEMBER_NAME_FILTER: left
package util

annotation class Anno(val str: String)
const val constant = 0
data class MyPair(val a: @Anno(0 + constant) List<@Anno(1 + constant) List<@Anno(2 + constant) Int>>, val b: Int)
val pair: MyPair

val (left, right) = pair