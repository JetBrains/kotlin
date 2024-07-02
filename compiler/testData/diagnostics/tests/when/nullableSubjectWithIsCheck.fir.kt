// ISSUE: KT-49710

fun Int?.isNull() = when (this) {
    null -> true
    <!USELESS_IS_CHECK!>is Int<!> -> false
}