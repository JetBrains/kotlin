// RESOLVE_SCRIPT
// MEMBER_NAME_FILTER: <destruct>
var a = -1
var b = 0

data class MyPair(val i: Int, val b: Int)

val pair = MyPair(a, b)

val (first, last) = pair