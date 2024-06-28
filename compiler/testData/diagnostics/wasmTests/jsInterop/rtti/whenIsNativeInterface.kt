// FIR_IDENTICAL
external interface I

external interface J

fun box(a: Any) = when (a) {
    <!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>is I<!> -> 0
    <!CANNOT_CHECK_FOR_EXTERNAL_INTERFACE!>!is J<!> -> 1
    else -> 2
}