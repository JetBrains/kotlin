// FIR_IDENTICAL

fun ff(l: Any) = when(l) {
    is <!CANNOT_CHECK_FOR_ERASED!>MutableList<String><!> -> 1
    else <!SYNTAX!>2<!>
}
