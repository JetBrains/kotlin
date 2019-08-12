// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 3
 * NUMBER: 1
 * DESCRIPTION: Exhaustive when using boolean values.
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
