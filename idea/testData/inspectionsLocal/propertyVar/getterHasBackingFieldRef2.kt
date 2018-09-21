// PROBLEM: none
class Test {
    private <caret>var p: Int = 0
        get() {
            foo(field)
            return 1
        }

    private fun foo(i: Int) {}
}