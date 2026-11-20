class B {
    operator fun get(vararg indices: Int): B = this
    operator fun set(vararg indices: Int, value: B) {}
    operator fun plus(other: Int): B = this
}

fun test(b: B) {
    b[1, 2, 3] <caret>+= 1
}
