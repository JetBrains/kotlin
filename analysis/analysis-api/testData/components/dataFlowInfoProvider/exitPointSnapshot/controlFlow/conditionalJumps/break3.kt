fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <expr>if (a + b > 0) break
        val c: Int
        consume(a - b)
        if (a - b > 0) break
        consume(a + b)</expr>
        c = 1
        consume(c)
    }
    return 1
}

fun consume(obj: Any?) {}