// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and 'when condition' with contains operator and type without defined contains operator.
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: EmptyClass, value_3: Int, value_4: Any): String {
    when (value_1) {
        <!NONE_APPLICABLE!>in<!> value_2  -> return ""
        <!NONE_APPLICABLE!>in<!> value_3  -> return ""
        <!NONE_APPLICABLE!>in<!> value_4  -> return ""
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
