fun foo(f: (Int, Int, Int) -> Unit) {
    f(1, 2, 3)
}

fun bar() {
    foo { _, <caret>_, _ ->  }
}
