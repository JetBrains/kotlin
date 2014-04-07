fun foo() {
    @outer while (true) {
        @inner do {
            cont<caret>inue
        } while (false)
    }
}
