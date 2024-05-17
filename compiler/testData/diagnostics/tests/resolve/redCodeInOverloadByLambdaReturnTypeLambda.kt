// WITH_STDLIB
private fun foo(seq: Sequence<String>) {
    // flatMap should not be unresolved
    seq.<!CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION!>flatMap <!TYPE_MISMATCH!>{ it.<!UNRESOLVED_REFERENCE!>length2<!> }<!><!>
}