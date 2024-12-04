fun foo(a: Int): Int {
    val b: Int = 1
    <expr>if (a + b > 0) return 0
    else if (a - b < 0) consume(a - b)
    else consume(0)</expr>
    return 1
}

fun consume(obj: Any?) {}