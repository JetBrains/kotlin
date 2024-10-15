// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL

fun ff(l: Any) = l is <!CANNOT_CHECK_FOR_ERASED!>MutableList<String><!>
