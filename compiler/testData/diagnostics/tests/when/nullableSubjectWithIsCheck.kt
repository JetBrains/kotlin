// ISSUE: KT-49710

fun Int?.isNull() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    null -> true
    <!USELESS_IS_CHECK!>is Int<!> -> false
}
