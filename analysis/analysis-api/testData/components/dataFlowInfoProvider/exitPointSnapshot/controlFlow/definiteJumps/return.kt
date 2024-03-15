fun foo(a: Int): Int {
    val b: Int = 1
    <expr>if (a + b > 0) return 1
    else if (a - b < 0) return 2
    else return b</expr>
}