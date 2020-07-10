fun foo(i: Int) {
    if (i == 1) {
        bar(1)
    } else <caret>if (i == 2) {
        bar(2)
    } else {
        bar(3)
    }
}

fun bar(i: Int) {}