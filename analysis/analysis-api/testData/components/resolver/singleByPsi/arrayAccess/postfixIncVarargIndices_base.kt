class B {
    operator fun get(vararg indices: Int): B = this
    operator fun set(vararg indices: Int, value: B) {}
    operator fun inc(): B = this
}

fun test(b: B) {
    <expr>b[1, 2, 3]</expr>++
}
