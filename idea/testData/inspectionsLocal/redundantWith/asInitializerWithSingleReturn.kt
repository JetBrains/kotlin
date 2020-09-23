// WITH_RUNTIME
fun foo(s: String): Int =
    <caret>with("s") {
        return 42
    }