fun foo(a: Int): Int {
    val b: Int = 1
    <expr>if (a + b > 0) return 0
    consume(a - b)</expr>
    return 1
}

fun consume(obj: Any?) {}