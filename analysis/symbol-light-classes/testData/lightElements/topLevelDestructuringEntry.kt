// KT-90199
data class X(val a: Int, val b: String)

val (a, <caret>b) = X(1, "")
