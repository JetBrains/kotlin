// !WITH_NEW_INFERENCE
// AssertionError for nested ifs with lambdas and Nothing as results
// NI_EXPECTED_FILE

val fn = if (true) {
    { true }
} 
else if (true) {
    { true }
}
else {
    null!!
}