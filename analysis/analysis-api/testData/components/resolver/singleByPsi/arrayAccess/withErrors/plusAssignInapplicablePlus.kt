class B {
    operator fun get(i: Int): B = this
    operator fun set(i: Int, value: B) {}
    operator fun plus(other: String): B = this
}

fun test(b: B) {
    b[0] <caret>+= 1
}
