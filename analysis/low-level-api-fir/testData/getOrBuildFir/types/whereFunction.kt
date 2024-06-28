interface One
interface Two

fun <T> foo(t: T) where T : One, T : <expr>Two</expr> = t