// WITH_RUNTIME
fun test(): Int = <caret>with("") {
    println()
    return 42
}
