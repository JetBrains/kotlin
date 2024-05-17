// WITH_STDLIB
private fun foo(seq: Sequence<String>) {
    // flatMap should not be unresolved
    seq.<!UNRESOLVED_REFERENCE!>flatMap<!> <!UNRESOLVED_REFERENCE!>{ it.<!UNRESOLVED_REFERENCE!>length2<!> }<!>
}