class B {
    operator fun get(i: Int): B = this
    operator fun set(i: Int, value: String) {}
    operator fun set(i: Int, value: Long) {}
    operator fun inc(): B = this
}

fun test(b: B) {
    b[0]<caret>++
}
