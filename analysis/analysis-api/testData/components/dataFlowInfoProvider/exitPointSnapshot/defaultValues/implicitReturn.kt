inline fun foo(a: Int, f: (Int) -> Int) = f(a)

fun bar(a: Int): Int = foo(a) { <expr>if (it > 0) it else return 0</expr> }