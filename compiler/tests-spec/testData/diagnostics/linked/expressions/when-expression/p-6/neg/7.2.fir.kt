// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 7
 * NUMBER: 2
 * DESCRIPTION: 'When' without bound value and with 'else' branch not in the last position.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = when {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    value_1 == 1 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = when {
    value_1 == 1 -> ""
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    value_1 == 2 -> ""
}

// TESTCASE NUMBER: 3
fun case_3(): String {
    when {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> return ""
        else -> return ""
    }

    return ""
}
