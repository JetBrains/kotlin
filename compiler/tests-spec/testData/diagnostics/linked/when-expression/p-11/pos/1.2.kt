// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Check when exhaustive via else entry (when with bound value).
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = when (value_1) {
    0 -> ""
    1 -> ""
    2 -> ""
    3 -> ""
    else -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean): String = when (value_1) {
    true -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 3
 * NOTE: for a potential bound value constant analysis.
 */
fun case_3(): String = when (true) {
    true -> ""
    else -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int): String = when(value_1) {
    else -> ""
}