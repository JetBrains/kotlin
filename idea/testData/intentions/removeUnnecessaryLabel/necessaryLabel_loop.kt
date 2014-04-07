// IS_APPLICABLE: false
fun foo() {
    @outer while (true) {
        while (false) {
            break@ou<caret>ter
        }
    }
}
