// IGNORE_REVERSED_RESOLVE
class A(foo: Int.() -> Unit) {
    init {
        4.foo()
    }
}

fun test(foo: Int.(String) -> Unit) {
    4.foo("")
    4.foo(<!NO_VALUE_FOR_PARAMETER!>p1 = "")<!>
    4.foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p2<!> = "")
}
