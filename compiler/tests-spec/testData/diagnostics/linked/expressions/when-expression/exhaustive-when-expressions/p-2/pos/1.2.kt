// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-100
 * MAIN LINK: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: Exhaustive when, with bound value, with else branch.
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