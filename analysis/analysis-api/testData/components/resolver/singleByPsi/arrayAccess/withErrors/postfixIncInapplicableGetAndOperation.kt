class B {
    operator fun get(i: String): B = this
    operator fun set(i: Int, value: B) {}
}

fun test(b: B) {
    b[0]<caret>++
}
