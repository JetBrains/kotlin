class A(foo: Int.() -> Unit) {
    init {
        4.foo()
    }
}

fun test(foo: Int.(String) -> Unit) {
    4.foo("")
    4.foo(<!NO_VALUE_FOR_PARAMETER!><!ARGUMENT_PASSED_TWICE, NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")<!>
    4.foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p2<!> = "")
}
