/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 1
 * DESCRIPTION: Check when exhaustive when boolean values are checked and contains a null check.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean?): String = when (value_1) {
    true -> ""
    false -> ""
    null -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean?): String = when (value_1) {
    true && false && ((true || false)) || true && !!!false && !!!true -> ""
    true && false && ((true || false)) || true && !!!false -> ""
    null -> ""
}
