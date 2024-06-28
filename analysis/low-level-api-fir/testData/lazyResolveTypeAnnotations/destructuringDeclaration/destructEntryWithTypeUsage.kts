package util

annotation class Anno(val str: String)
const val constant = 0
data class MyPair(val a: List<List<Int>>, val b: Int)
val pair: MyPair

val (left: @Anno(0 + constant) List<@Anno(1 + constant) List<@Anno(2 + constant) Int>>, right) = pair
val prop<caret>erty = left