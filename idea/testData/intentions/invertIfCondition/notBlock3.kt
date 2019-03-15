fun foo(i: Int) {
    <caret>if (i > 0)
        bar()
    else {
    }
}

fun bar() {}