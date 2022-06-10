// FIR_IDENTICAL
// AssertionError for nested ifs with lambdas and Nothing as results

val fn = if (true) {
    { true }
}
else if (true) {
    { true }
}
else {
    null!!
}
