// ERROR: A 'return' expression required in a function with a block body ('{...}')
// PROBLEM: none

enum class TestEnum{
    A, B, C
}

fun test(e: TestEnum): Int {
    <caret>when (e) {
        TestEnum.A -> return 1
        TestEnum.B -> return 2
    }
}