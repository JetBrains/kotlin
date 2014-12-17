fun foo() {
    <caret>if (a) {
        if (b) foo()
    }
    else {
        bar()
    }
}
