// WITH_RUNTIME
// SKIP_ERRORS_AFTER
// TODO: 'return' is not allowed here
fun foo() {
    <caret>assert(true) {
        return
    }
}