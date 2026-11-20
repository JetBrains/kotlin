class B {
    operator fun get(vararg indices: Int): Int = 0
    operator fun get(index: Any): Int = 0
    operator fun set(i: Int, j: Int, value: Int) {}
}

fun test(b: B) {
    b[1, 2] <caret>+= 1
}
