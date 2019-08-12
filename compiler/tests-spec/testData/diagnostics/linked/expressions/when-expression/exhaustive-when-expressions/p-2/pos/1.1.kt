// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Exhaustive when, without bound value, with else branch.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = when {
    value_1 == 0 -> ""
    value_1 > 0 && value_1 <= 10 -> ""
    value_1 > 10 && value_1 <= 100 -> ""
    value_1 > 100 -> ""
    else -> ""
}

// TESTCASE NUMBER: 2
fun case_2(): String = when {
    true -> ""
    else -> ""
}

// TESTCASE NUMBER: 3
fun case_3(): String = when {
    else -> ""
}