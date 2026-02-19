// IGNORE_FE10
// FE10 doesn't resolve the initializer and returns a Unit type here
var first = -1
var last = -1
val pair = Pair(first, last)

val (first, last) = <expr>pair</expr>