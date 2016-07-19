// "Implement interface" "true"
// WITH_RUNTIME
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

interface <caret>Base {
    fun foo(x: Int): Int

    fun bar(y: String) = y
}
