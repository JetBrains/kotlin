fun foo() {
    <caret>bar { }
}

fun bar(f: () -> Unit) = 1

// EXPECTED: null