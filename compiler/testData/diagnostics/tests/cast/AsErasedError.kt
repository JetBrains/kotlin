// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun ff(c: MutableCollection<String>) = c <!UNCHECKED_CAST!>as MutableList<Int><!>
