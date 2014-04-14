fun foo(): Int {
    val a = 10

    return <caret>if (a > 0) {
        a
    } else {
        a + 1
    }
}