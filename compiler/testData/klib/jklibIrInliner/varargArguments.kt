// A vararg argument is not substituted: it would allocate a new array at each use.

inline fun same(vararg xs: Int): Boolean = xs === xs

fun test(): Boolean = same(1, 2)
