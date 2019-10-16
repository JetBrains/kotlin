fun foo() {
    1 <caret>foo 1
}

fun Int.foo(i: Int) = 1

// EXPECTED: 1 foo 1