// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun f(a: MutableList<in String>) = a is <!CANNOT_CHECK_FOR_ERASED!>MutableList<CharSequence><!>