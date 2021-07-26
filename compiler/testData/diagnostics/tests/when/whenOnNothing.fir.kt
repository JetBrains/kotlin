// !DIAGNOSTICS: -UNREACHABLE_CODE

// exhaustive
fun test1(n: Nothing) = when (n) { }
fun test2(n: Nothing?) = when (n) {
    null -> {}
}

// not exhaustive
fun test3(n: Nothing?) = <!NO_ELSE_IN_WHEN!>when<!> (n) {
}
