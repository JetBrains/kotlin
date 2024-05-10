// DIAGNOSTICS: -UNUSED_PARAMETER

fun String.invoke(i: Int) {}

fun foo(i: Int) {
    <!UNRESOLVED_REFERENCE!>i<!>(1)

    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>1<!>(1)
}
