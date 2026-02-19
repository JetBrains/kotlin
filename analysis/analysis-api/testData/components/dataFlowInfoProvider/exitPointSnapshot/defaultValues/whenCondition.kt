fun foo(a: Int): Int {
    val b: Int = 1
    return when (a + b) {
        <expr>0</expr> -> b
        1 -> -b
        else -> a - b
    }
}