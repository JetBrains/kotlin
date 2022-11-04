const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!><!UNSIGNED_LITERALS_NOT_PRESENT!>1u<!> + <!UNSIGNED_LITERALS_NOT_PRESENT!>2u<!><!>

fun box() = when {
    c != <!UNSIGNED_LITERALS_NOT_PRESENT!>3u<!> -> "fail"
    else -> "OK"
}
