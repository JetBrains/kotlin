/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and not allowed break and continue expression (without labels) in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with break expression (without label).
fun case_1(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            <!BREAK_OR_CONTINUE_IN_WHEN!>break<!><!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue expression (without label).
fun case_2(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!><!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue (first) and break (second) expressions (without label).
fun case_3(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            <!BREAK_OR_CONTINUE_IN_WHEN!>continue<!><!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
            <!UNREACHABLE_CODE!><!BREAK_OR_CONTINUE_IN_WHEN!>break<!> -> return ""<!>
        }
    }

    return ""
}

// CASE DESCRIPTION: 'When' with continue (second) and break (first) expressions (without label).
fun case_4(value: Int, value1: MutableList<Int>): String {
    while (value1.isNotEmpty()) {
        value1.removeAt(0)
        when (value) {
            <!BREAK_OR_CONTINUE_IN_WHEN!>break<!><!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
            <!UNREACHABLE_CODE!><!BREAK_OR_CONTINUE_IN_WHEN!>continue<!> -> return ""<!>
        }
    }

    return ""
}
