fun foo(a: Int) {
    val b: Int = 1
    for (n in 1..b) {
        <expr>if (a > 0) throw Exception("")
        if (a + b > 0) break
        consume(a - b)</expr>
    }
}

fun consume(obj: Any?) {}