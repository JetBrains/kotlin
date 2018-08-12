/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 5: Any other expression.
 NUMBER: 22
 DESCRIPTION: 'When' with bound value and return expression in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and only one return expression.
fun case_1(value: Any?): String {
    when (value) {
        return ""<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>else -> ""<!>
    }
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and only one return expression.
fun case_2(value: Any?): String {
    when (value) {
        return ""<!UNREACHABLE_CODE!><!> -> <!UNREACHABLE_CODE!>return ""<!>
    }
}

// CASE DESCRIPTION: 'When' with 'else' branch (as expression) and several return expressions.
fun case_3(value: Any?): String {
    when (value) {
        <!UNREACHABLE_CODE!>return return return return<!> return "" -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>return (return (return (return (return "")))) -> return ""<!>
        <!UNREACHABLE_CODE!>return "" -> return ""<!>
        <!UNREACHABLE_CODE!>return "" -> return ""<!>
        <!UNREACHABLE_CODE!>else -> ""<!>
    }

    <!UNREACHABLE_CODE!>return ""<!>
}

// CASE DESCRIPTION: 'When' without 'else' branch (as statement) and several throw expressions.
fun case_4(value: Any?): String {
    when (value) {
        <!UNREACHABLE_CODE!>return return return return<!> return "" -> <!UNREACHABLE_CODE!>return ""<!>
        <!UNREACHABLE_CODE!>return (return (return (return (return "")))) -> return ""<!>
        <!UNREACHABLE_CODE!>return "" -> return ""<!>
        <!UNREACHABLE_CODE!>return "" -> return ""<!>
    }
}
