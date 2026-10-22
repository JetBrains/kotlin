// KT-90199
// The file has no other top-level callables
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

var (c, d) = X(2, "")
