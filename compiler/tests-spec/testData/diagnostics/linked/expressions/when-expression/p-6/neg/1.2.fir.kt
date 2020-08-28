// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Any, value_2: Int): String {
    when (value_1) {
        is <!UNRESOLVED_REFERENCE!>value_2<!> -> return ""
    }

    return ""
}
