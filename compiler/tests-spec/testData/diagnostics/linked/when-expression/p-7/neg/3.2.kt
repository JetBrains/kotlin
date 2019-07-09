// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 3
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and 'when condition' with contains operator and type without defined contains operator.
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Int, value_2: EmptyClass, value_3: Int, value_4: Any): String {
    when (value_1) {
        <!TYPE_MISMATCH_IN_RANGE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>in<!> value_2  -> return ""
        <!TYPE_MISMATCH_IN_RANGE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>in<!> value_3  -> return ""
        <!TYPE_MISMATCH_IN_RANGE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>in<!> value_4  -> return ""
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
        <!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE, UNREACHABLE_CODE!>in<!> value_3 -> <!UNREACHABLE_CODE!>{}<!>
        <!UNREACHABLE_CODE!><!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE!>in<!> throw Exception() -> {}<!>
        <!UNREACHABLE_CODE!><!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE!>in<!> return -> {}<!>
    }
}
