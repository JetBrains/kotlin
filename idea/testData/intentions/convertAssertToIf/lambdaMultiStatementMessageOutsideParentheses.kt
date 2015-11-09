// WITH_RUNTIME
fun foo() {
    <caret>assert(true) {
        if (false) return
        "text"
    }
}