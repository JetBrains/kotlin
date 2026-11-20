class B {
    operator fun get(i: Int, j: String): B = this
    operator fun set(i: Int, j: String, value: B) {}
    operator fun plus(other: Int): B = this
}

fun test(b: B) {
    <expr>b[0, "a"]</expr> += 1
}
