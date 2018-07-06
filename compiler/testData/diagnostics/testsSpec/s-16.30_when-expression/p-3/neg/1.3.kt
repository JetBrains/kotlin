// SKIP_TXT

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 3
 DESCRIPTION: 'When' without bound value and not allowed break and continue expression (without labels) in the control structure body.
 */

// CASE DESCRIPTION: 'When' with break expression (without label).
fun case_1(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when {
            value == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue expression (without label).
fun case_2(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when {
            value == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with with continue (first) and break (second) expression (without label).
fun case_3(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when {
            value == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
            value == 2 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with with continue (second) and break (first) expression (without label).
fun case_4(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when {
            value == 1 -> <!BREAK_OR_CONTINUE_IN_WHEN!>break<!>
            value == 2 -> <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!>
        }
    }

    return ""
}
