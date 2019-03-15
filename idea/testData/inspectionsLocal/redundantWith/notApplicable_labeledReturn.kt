// PROBLEM: none
// WITH_RUNTIME
fun test() {
    <caret>with ("") {
        return@with
    }
}