fun test(i: Int) {
    val f: () -> Boolean
    <caret>if (i == 1) {
        f = { true }
    } else {
        foo { i }
        f = { false }
    }
    f()
}

fun foo(f: () -> Int) {}