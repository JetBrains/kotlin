enum class TestEnum{
    A, B, C
}

fun test(e: TestEnum): Int {
    <caret>when (e) {
        TestEnum.A -> return 1
        TestEnum.B -> return 2
        else -> return 3
    }
}