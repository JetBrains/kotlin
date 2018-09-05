// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [3] Contains test condition: containment operator followed by an expression.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and 'when condition' with contains operator and type without defined contains operator.
 */

// CASE DESCRIPTION: 'When' with values of types without defined contains operator.
fun case_1(value: Int, value1: _EmptyClass, value2: Int, value3: Any): String {
    when (value) {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value1  -> return ""
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value2  -> return ""
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value3  -> return ""
    }

    return ""
}

/*
 CASE DESCRIPTION: 'When' with values of Nothing (all existing contains operators used here).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-25948
 */
fun case_2(value: Int, value3: Nothing) {
    when (value) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE, UNREACHABLE_CODE!>in<!> value3 -> <!UNREACHABLE_CODE!>{}<!>
        <!UNREACHABLE_CODE!><!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE!>in<!> throw Exception() -> {}<!>
        <!UNREACHABLE_CODE!><!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE!>in<!> return -> {}<!>
    }
}
