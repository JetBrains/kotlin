fun test(b: Boolean): Int {
    <caret>return if (b) {
        throw AssertionError()
    } else {
        1
    }
}