// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(l: () -> Unit) {}
fun bar(l: () -> String) {}

val a = foo { <!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!> }
val b = bar { <!ARGUMENT_TYPE_MISMATCH, UNSUPPORTED!>[]<!> }
