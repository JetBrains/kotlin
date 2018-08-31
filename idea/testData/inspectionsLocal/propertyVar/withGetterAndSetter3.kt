// PROBLEM: none
class Test {
    private <caret>var p: Int = foo()
        get() {
            return foo()
        }
        set(value) {
            foo()
            field = value
        }

    private fun foo() = 1
}