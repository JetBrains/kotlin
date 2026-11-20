class B {
    operator fun get(i: String): B = this
    operator fun set(i: Int, value: B) {}
    operator fun inc(): B = this
}

fun test(b: B) {
    b[0]<caret>++
}
