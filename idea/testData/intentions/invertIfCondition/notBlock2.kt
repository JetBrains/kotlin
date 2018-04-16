fun foo(i: Int): Int {
    return <caret>if (i > 0) {
        i
    } else
        i + 1
}