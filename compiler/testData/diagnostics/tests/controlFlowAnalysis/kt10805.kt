// !WITH_NEW_INFERENCE
// AssertionError for nested ifs with lambdas and Nothing as results
// NI_EXPECTED_FILE

val <!OI;IMPLICIT_NOTHING_PROPERTY_TYPE!>fn<!> = if (true) {
    <!OI;TYPE_MISMATCH!>{ true }<!>
} 
else if (true) {
    <!OI;TYPE_MISMATCH!>{ true }<!>
}
else {
    null!!
}