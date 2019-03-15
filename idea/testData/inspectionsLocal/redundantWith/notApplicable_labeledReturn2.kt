// PROBLEM: none
// WITH_RUNTIME
fun test(): String {
    <caret>with("") {
        return this@with
    }
}