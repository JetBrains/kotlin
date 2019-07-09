/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Check when exhaustive via boolean bound value and evaluating to value true and false.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean): String = when (value_1) {
    true -> ""
    false -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean): String = when (value_1) {
    true && false && ((true || false)) || true && !!!false && !!!true -> ""
    true && false && ((true || false)) || true && !!!false -> ""
}
