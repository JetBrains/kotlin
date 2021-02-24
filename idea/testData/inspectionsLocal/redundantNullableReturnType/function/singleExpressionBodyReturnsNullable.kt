// PROBLEM: none
// WITH_RUNTIME
fun foo(xs: List<Int>, b: Boolean): Int?<caret> = if (b) xs.first() else xs.lastOrNull()