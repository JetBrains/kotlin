// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 10
 * NUMBER: 1
 * DESCRIPTION: Non-exhaustive when using nullable boolean values.
 */


// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
    false -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    true -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Boolean?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }
