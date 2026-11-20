class B {
    operator fun get(i: String): B = this
    operator fun set(i: Boolean, value: String) {}
}

fun test(b: B) {
    b[0]<caret>++
}
