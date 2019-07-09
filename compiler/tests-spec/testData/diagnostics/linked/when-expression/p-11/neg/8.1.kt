// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 1
 * DESCRIPTION: Checking for not exhaustive 'when' on the nullable Boolean.
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
