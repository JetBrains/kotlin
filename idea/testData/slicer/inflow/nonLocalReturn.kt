// FLOW: IN

inline fun foo(a: Int, f: (Int) -> Int) = f(a)

fun <caret>bar(a: Int): Int = foo(a) { if (it > 0) it else return 0 }