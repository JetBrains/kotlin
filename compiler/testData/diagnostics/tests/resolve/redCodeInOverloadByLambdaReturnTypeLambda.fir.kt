// WITH_STDLIB
private fun foo(seq: Sequence<String>) {
    // flatMap should not be unresolved
    seq.<!CANNOT_INFER_PARAMETER_TYPE!>flatMap<!> <!UNRESOLVED_REFERENCE!>{ it.<!UNRESOLVED_REFERENCE!>length2<!> }<!>
}