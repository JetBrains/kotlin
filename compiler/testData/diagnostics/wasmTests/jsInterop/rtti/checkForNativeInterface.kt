// FIR_IDENTICAL
external interface I

fun box(a: Any, b: Any): Boolean {
    return <!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>a is I<!> && <!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>b !is I<!>
}