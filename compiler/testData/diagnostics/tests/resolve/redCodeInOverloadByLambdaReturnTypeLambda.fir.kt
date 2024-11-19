// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
private fun foo(seq: Sequence<String>) {
    // flatMap should not be unresolved
    seq.<!UNRESOLVED_REFERENCE!>flatMap<!> { it.<!UNRESOLVED_REFERENCE!>length2<!> }
}
