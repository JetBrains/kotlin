// !DIAGNOSTICS: -UNUSED_PARAMETER

fun String.invoke(i: Int) {}

fun foo(i: Int) {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>i<!>(1)

    <!CALLEE_NOT_A_FUNCTION!>1<!>(1)
}