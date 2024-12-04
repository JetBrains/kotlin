package util

annotation class Anno(val position: String)
data class Pair(val a: Int, val b: Int)
const val prop = "str"

@Anno("destr $prop")
v<caret>al (@Anno("a $prop") a, @Anno("b $prop") b) = Pair(0, 1)
