// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-100
 * PLACE: expressions, when-expression -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: Forbidden break and continue in the control structure body of when.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): String {
    while (true) {
        when {
            value_1 == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): String {
    while (true) {
        when {
            value_1 == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
        }
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
