fun foo(f: (a: Int, b: Int, c: Int) -> Unit) {
    f(1, 2, 3)
}

fun bar() {
    foo { _, <caret>_, _ ->  }
}
