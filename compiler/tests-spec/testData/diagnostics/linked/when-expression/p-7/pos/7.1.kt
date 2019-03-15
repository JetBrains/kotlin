/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 7
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and else branch.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int?) = when (value_1) {
    0 -> ""
    1 -> ""
    2 -> ""
    else -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any) = when (value_1) {
    is Int -> ""
    is Boolean -> ""
    is String -> ""
    else -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int) = when (value_1) {
    in -10..10 -> ""
    in 11..1000 -> ""
    in 1000..Int.MAX_VALUE -> ""
    else -> ""
}