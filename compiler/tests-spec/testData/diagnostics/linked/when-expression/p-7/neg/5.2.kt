/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 7
 SENTENCE: [5] Any other expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and not allowed break and continue expression (without labels) in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with break expression (without label).
fun case_1(value_1: Int): String {
    while (true) {
        when (value_1) {
            <!BREAK_OR_CONTINUE_IN_WHEN!>break<!><!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue expression (without label).
fun case_2(value_1: Int): String {
    while (true) {
        when (value_1) {
            <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!><!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        }
    }

    <!UNREACHABLE_CODE!>return ""<!>
}
