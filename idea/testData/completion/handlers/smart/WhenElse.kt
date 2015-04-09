fun foo(s: String) {
    when(s) {
        "" -> return
        <caret>
    }
}

// ELEMENT: else
