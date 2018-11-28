/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 6
 * SENTENCE: [1] When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and not allowed break and continue expression (without labels) in the control structure body.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int): Int {
    while (true) {
        when (value_1) {
            1 -> return 1
            2 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return 0
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int): Int {
    while (true) {
        when (value_1) {
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
            2 -> return 1
        }
    }

    <!UNREACHABLE_CODE!>return 0<!>
}
