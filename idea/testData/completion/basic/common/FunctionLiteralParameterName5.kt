fun bar() {
    val handler = { <caret>
        foo()
    }
}

// INVOCATION_COUNT: 0
// NUMBER: 0
