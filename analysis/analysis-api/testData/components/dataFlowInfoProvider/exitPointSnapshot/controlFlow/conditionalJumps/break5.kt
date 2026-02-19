fun foo(a: Int): Int {
    val b: Int = 1
    for (n in 1..a) {
        <expr>when {
            a + b > 0 -> break
            a - b > 0 -> break
            else -> consume(0)
        }</expr>
    }
    return 1
}

fun consume(obj: Any?) {}