// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// ISSUE: KT-39883

// Should always work
fun test_0(b: Boolean): String = when (b) {
    true -> "true"
    false -> "false"
}

// Deprecated
fun test_1(b: Boolean): String = when (b) {
    (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>1 == 1<!>) -> "true"
    (<!NON_TRIVIAL_BOOLEAN_CONSTANT!>"" != ""<!>) -> "false"
}

const val TRUE = true

// Already not working
fun test_2(b: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!>(b) {
    TRUE -> "true"
    false -> "false"
}

const val s1 = "s1"
const val s2 = "s2"

// Already not working
fun test_3(b: Boolean): String = <!NO_ELSE_IN_WHEN!>when<!>(b) {
    true -> "true"
    (s1 == s2) -> "false"
}
