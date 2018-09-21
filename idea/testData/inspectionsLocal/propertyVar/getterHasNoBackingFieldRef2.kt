// FIX: Change to val and delete initializer
class Test {
    private <caret>var p: Int = 0
        get() {
            foo()
            return bar()
        }

    private fun foo() {}

    private fun bar() = 2
}