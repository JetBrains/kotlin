// !DIAGNOSTICS: -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 5
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and non-expressions in 'when condition'.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: List<Int>): String {
    when (value_1) {
        <!EXPRESSION_EXPECTED!>while (false) {}<!> -> return ""
        <!EXPRESSION_EXPECTED!>do {} while (false)<!> -> return ""
        <!EXPRESSION_EXPECTED!>for (value in value_2) {}<!> -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int): String {
    var value_2: Int
    var value_3 = 10

    when (value_1) {
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>value_2 = 10<!> -> return ""
        <!ASSIGNMENT_IN_EXPRESSION_CONTEXT!>value_3 %= 10<!> -> return ""
    }

    return ""
}