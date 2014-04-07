// IS_APPLICABLE: false
fun foo() {
    @outer while (true) {
        do {
            cont<caret>inue
        } while (false)
    }
}
