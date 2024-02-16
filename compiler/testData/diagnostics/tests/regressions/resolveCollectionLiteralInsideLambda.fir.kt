// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(l: () -> Unit) {}
fun bar(l: () -> String) {}

val a = foo { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, UNSUPPORTED!>[]<!> }
val b = bar { <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH, UNSUPPORTED!>[]<!> }
