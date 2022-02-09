// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(l: () -> Unit) {}
fun bar(l: () -> String) {}

val a = foo { <!UNSUPPORTED!>[]<!> }
val b = bar { <!UNSUPPORTED!>[]<!> }
