// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun ff(a: Any) = a <!UNCHECKED_CAST!>as MutableList<String><!>
