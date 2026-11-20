class B {
    operator fun get(i: Int): Int = i
    operator fun set(i: Int, value: String) {}
}

fun test(b: B) {
    <expr>b[0]</expr> += 1
}
