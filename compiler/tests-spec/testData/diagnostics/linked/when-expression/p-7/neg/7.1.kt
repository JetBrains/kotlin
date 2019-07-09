// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 7
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and with else branch not in the last position.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String = when (value_1) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>1 -> ""<!>
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String = when (value_1) {
    1 -> ""
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> ""
    <!UNREACHABLE_CODE!>2 -> ""<!>
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int): String {
    when (value_1) {
        <!ELSE_MISPLACED_IN_WHEN!>else<!> -> return ""
        <!UNREACHABLE_CODE!>else -> return ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
