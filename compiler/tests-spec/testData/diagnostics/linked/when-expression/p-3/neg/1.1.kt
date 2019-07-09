/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 3 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' without bound value and not allowed break and continue expression (without labels) in the control structure body.
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
