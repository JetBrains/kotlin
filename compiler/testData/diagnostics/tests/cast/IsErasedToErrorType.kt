// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun testing(a: Any) = a is <!UNRESOLVED_REFERENCE!>UnresolvedType<!><Int>
