// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 1 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Empty when with bound value.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int) {
    when (<!UNUSED_EXPRESSION!>value_1<!>) {}
}
