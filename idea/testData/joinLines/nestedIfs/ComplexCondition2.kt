fun foo() {
    <caret>if (a && b) {
        if (c || d) foo()
    }
}
