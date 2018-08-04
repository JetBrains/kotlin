/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 4
 DESCRIPTION: 'When' with bound value and not allowed break and continue expression (without labels) in the control structure body.
 */

// CASE DESCRIPTION: 'When' with break expression (without label).
fun case_1(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue expression (without label).
fun case_2(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with with continue (first) and break (second) expression (without label).
fun case_3(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
            2 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with with continue (second) and break (first) expression (without label).
fun case_4(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
            2 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
        }
    }

    return ""
}