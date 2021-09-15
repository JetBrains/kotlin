// FIR_IDENTICAL
fun f(a: MutableList<String>) = a is <!CANNOT_CHECK_FOR_ERASED!>MutableList<CharSequence><!>