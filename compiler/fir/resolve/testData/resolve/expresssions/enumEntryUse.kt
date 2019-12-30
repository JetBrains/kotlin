enum class TestEnum {
    FIRST,
    SECOND,
    THIRD {
        fun foo() {}
    }
}

fun use(some: Any?) {}

fun useEnum(some: TestEnum) {}

fun useVararg(vararg some: TestEnum) {}

fun test() {
    use(TestEnum.FIRST)
    useEnum(TestEnum.SECOND)
    useEnum(TestEnum.THIRD)

    <!INAPPLICABLE_CANDIDATE!>useVararg<!>(TestEnum.FIRST, TestEnum.SECOND)
    <!INAPPLICABLE_CANDIDATE!>useVararg<!>(1, 2, 3, 4, 5)
}