/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Empty 'when' with bound value.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int) {
    when (<!UNUSED_EXPRESSION!>value_1<!>) {}
}
