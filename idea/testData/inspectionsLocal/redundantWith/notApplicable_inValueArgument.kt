// PROBLEM: none
// WITH_RUNTIME
fun foo(i: Int) {}

fun test() {
    foo(<caret>with("") {
        println()
        1
    })
}