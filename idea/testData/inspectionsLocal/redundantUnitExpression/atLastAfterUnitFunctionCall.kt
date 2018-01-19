fun test(b: Boolean): Unit = if (b) {
    unit()
    <caret>Unit
} else {
}

fun unit() {
}