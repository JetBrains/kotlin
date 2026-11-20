class B {
    operator fun get(i: Int, j: String): B = this
    operator fun set(i: Int, j: String, value: B) {}
    operator fun inc(): B = this
}

fun test(b: B) {
    b[0, "a"]<caret>++
}
