// FIX: Change to val and delete initializer
class Test {
    private <caret>var p: Int = foo()
        get() = foo()

    private fun foo() = 1
}