class B {
    operator fun get(i: Int): B = this
    operator fun set(i: Int, value: String) {}
    operator fun inc(): B = this
}

fun test(b: B) {
    ++<expr>b[0]</expr>
}
