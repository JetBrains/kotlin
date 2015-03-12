fun <T: Any, U> T.let(f: (T) -> U): U = f(this)

fun bar(): Int = 1

fun foo(n: Int): Int {
    try {
        if (n < 0) return 0
        n.let { return it }
    }
    finally {
        for (i in 1..2) { }
        return bar()
    }
}