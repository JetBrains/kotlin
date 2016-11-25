external interface I

fun box(a: Any, b: Any): Boolean {
    return <!CANNOT_CHECK_FOR_NATIVE_INTERFACE!>a is I<!> && <!CANNOT_CHECK_FOR_NATIVE_INTERFACE!>b !is I<!>
}