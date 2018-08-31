// PROBLEM: none
class Test {
    private <caret>var p: Int = foo()
        get() = bar()

    private fun foo() = 1
    private fun bar() = 2
}