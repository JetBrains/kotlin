fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <expr>if (a + b > 0) break
        consume(a - b)
        break</expr>
    }
    return 1
}

fun consume(obj: Any?) {}