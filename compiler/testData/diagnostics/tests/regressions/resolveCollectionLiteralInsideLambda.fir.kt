// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(l: () -> Unit) {}
fun bar(l: () -> String) {}

val a = foo { [] }
val b = bar { <!ARGUMENT_TYPE_MISMATCH!>[]<!> }
