// PROBLEM: none
// WITH_RUNTIME
fun test() {
    val b = 1 == <caret>with ("") {
        println()
        1
    }
}