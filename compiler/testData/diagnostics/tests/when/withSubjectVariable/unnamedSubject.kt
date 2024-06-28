// FIR_IDENTICAL
// ISSUE: KT-58458

fun box() =
    when (val<!SYNTAX!><!> = <!UNRESOLVED_REFERENCE!>x<!>) {
        in <!UNRESOLVED_REFERENCE!>y<!> -> ""
        else -> ""
    }
