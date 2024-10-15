// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun f(a: List<Number>) = <!USELESS_IS_CHECK!>a is List<Any><!>
