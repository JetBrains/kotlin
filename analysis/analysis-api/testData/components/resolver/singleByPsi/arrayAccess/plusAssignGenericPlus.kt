class B {
    operator fun get(i: Int): B = this
    operator fun set(i: Int, value: B) {}
    operator fun <T> plus(other: T): B = this
}

fun test(b: B) {
    b[0] <caret>+= "x"
}
