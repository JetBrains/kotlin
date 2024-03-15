fun foo(a: Int): Int {
    val b: Int = 1
    <expr>when (a + b) {
        0 -> return 0
        1 -> consume(1)
        else -> consume(2)
    }
    </expr>
    return 1
}

fun consume(obj: Any?) {}