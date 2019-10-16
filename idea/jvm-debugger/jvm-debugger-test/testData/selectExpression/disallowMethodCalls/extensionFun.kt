fun foo() {
    1.<caret>foo()
}

fun Int.foo() = 1

// EXPECTED: null