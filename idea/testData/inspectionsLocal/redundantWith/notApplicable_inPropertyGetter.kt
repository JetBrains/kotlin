// PROBLEM: none
// WITH_RUNTIME
val a: Int
    get() = <caret>with("") {
        println()
        1
    }