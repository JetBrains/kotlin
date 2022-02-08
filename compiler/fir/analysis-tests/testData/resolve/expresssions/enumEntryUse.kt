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

    useVararg(TestEnum.FIRST, TestEnum.SECOND)
    useVararg(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>, <!ARGUMENT_TYPE_MISMATCH!>3<!>, <!ARGUMENT_TYPE_MISMATCH!>4<!>, <!ARGUMENT_TYPE_MISMATCH!>5<!>)
}
