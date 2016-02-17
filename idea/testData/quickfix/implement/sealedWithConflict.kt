// "Implement sealed class" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION

sealed class <caret>Base {
    abstract fun foo(): Int

    class BaseImpl : Base() {
        override fun foo() = throw UnsupportedOperationException()
    }
}