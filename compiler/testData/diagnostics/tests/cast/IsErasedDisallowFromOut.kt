// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL

fun f(a : MutableList<out Any>) = a is <!CANNOT_CHECK_FOR_ERASED!>MutableList<out Int><!>
