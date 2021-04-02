// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: EmptyClass, value_3: Int, value_4: Any): String {
    when (value_1) {
        <!INAPPLICABLE_CANDIDATE!>in<!> value_2  -> return ""
        <!INAPPLICABLE_CANDIDATE!>in<!> value_3  -> return ""
        <!INAPPLICABLE_CANDIDATE!>in<!> value_4  -> return ""
    }

    return ""
}

/*
 * TESTCASE NUMBER: 2
 * DISCUSSION
 * ISSUES: KT-25948
 */
fun case_2(value_1: Int, value_3: Nothing) {
    when (value_1) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>in<!> value_3 -> {}
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>in<!> throw Exception() -> {}
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>in<!> return -> {}
    }
}
