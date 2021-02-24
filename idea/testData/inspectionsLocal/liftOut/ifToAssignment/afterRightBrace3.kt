fun test(i: Int) {
    val f: () -> Boolean
    <caret>if (i == 1) {
        f = { true }
    } else {
        val g: () -> Boolean = { false }
        f = { g() }
    }
    f()
}