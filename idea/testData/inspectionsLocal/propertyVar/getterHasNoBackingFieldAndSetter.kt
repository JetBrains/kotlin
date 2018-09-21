// PROBLEM: none
class Test {
    private <caret>var p: Int = 0
        get() = 1
        set(value) {
            foo()
            field = value
        }

    private fun foo() {
    }
}