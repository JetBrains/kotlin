// WITH_RUNTIME
fun foo(s: String): Int {
    <caret>with("s") {
        println()
        return 42
    }
}