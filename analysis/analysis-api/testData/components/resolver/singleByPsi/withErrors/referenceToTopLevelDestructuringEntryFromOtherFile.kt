// KT-90199
// FILE: declarations.kt
data class X(val a: Int, val b: String)

val (a, b) = X(1, "")

// FILE: usages.kt
fun use() = <caret>b
