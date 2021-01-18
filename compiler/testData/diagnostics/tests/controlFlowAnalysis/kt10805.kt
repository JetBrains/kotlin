// !WITH_NEW_INFERENCE
// AssertionError for nested ifs with lambdas and Nothing as results
// NI_EXPECTED_FILE

val <!IMPLICIT_NOTHING_PROPERTY_TYPE{OI}!>fn<!> = if (true) {
    <!TYPE_MISMATCH{OI}!>{ true }<!>
} 
else if (true) {
    <!TYPE_MISMATCH{OI}!>{ true }<!>
}
else {
    null!!
}
