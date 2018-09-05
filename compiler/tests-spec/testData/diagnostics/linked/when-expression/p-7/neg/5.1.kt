// !DIAGNOSTICS: -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [5] Any other expression.
 NUMBER: 1
 DESCRIPTION: 'When' with bound value and non-expressions in 'when condition'.
 */

// CASE DESCRIPTION: 'When' with cycles in when condition.
fun case_1(value: Int, value1: List<Int>): String {
    when (value) {
        <!EXPRESSION_EXPECTED!>while (false) {}<!> -> return ""
        <!EXPRESSION_EXPECTED!>do {} while (false)<!> -> return ""
        <!EXPRESSION_EXPECTED!>for (value2 in value1) {}<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with assignments in when condition.
fun case_4(value: Int): String {
    var value1: Int
    var value2 = 10

    when (value) {
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>value1 = 10<!> -> return ""
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>value2 %= 10<!> -> return ""
    }

    return ""
}