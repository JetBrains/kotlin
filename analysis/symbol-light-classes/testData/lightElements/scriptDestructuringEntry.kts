// KT-90199
data class X(val a: Int, val b: String)

var (a, <caret>b) = X(1, "")
