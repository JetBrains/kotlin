// !WITH_NEW_INFERENCE
// AssertionError for nested ifs with lambdas and Nothing as results

val <!IMPLICIT_NOTHING_PROPERTY_TYPE!>fn<!> = if (true) {
    <!TYPE_MISMATCH!>{ true }<!>
} 
else if (true) {
    <!TYPE_MISMATCH!>{ true }<!>
}
else {
    null!!
}