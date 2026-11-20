class B {
    operator fun get(i: String): B = this
    operator fun set(i: Int, value: B) {}
    operator fun plus(other: String): B = this
}

fun test(b: B) {
    <expr>b[0]</expr> += 1
}
