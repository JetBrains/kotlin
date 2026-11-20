class B {
    operator fun get(i: String): B = this
    operator fun set(i: Boolean, value: String) {}
}

fun test(b: B) {
    <expr>b[0]</expr>++
}
