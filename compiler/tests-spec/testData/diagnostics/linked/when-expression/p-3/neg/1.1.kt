/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 3
 SENTENCE: [1] When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 1
 DESCRIPTION: 'When' without bound value and not allowed break and continue expression (without labels) in the control structure body.
 */

// CASE DESCRIPTION: 'When' with break expression (without label).
fun case_1(value: Int): String {
    while (true) {
        when {
            value == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue expression (without label).
fun case_2(value: Int): String {
    while (true) {
        when {
            value == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
        }
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
