// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Non-exhaustive when, with bound value, without else branch.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    1 -> ""
    2 -> ""
    3 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    1 -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int): Int = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {}
