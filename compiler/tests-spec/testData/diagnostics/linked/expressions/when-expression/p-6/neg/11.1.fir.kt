// DIAGNOSTICS: -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: List<Int>): String {
    when (value_1) {
        <!EXPRESSION_REQUIRED!>while (false) {}<!> -> return ""
        <!EXPRESSION_REQUIRED!>do {} while (false)<!> -> return ""
        for (value in value_2) {} -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int): String {
    var value_2: Int
    var value_3 = 10

    when (value_1) {
        <!EXPRESSION_REQUIRED!>value_2 = 10<!> -> return ""
        <!EXPRESSION_REQUIRED!>value_3 %= 10<!> -> return ""
    }

    return ""
}
