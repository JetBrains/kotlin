// Only the arguments with side effects are stored in temporary variables.

inline fun twice(x: Int): Int = x + x

fun sideEffect(): Int = 1

fun test(a: Int): Int = twice(a) + twice(sideEffect())
