// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_CLASSES

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 7
 SENTENCE 3: Contains test condition: containment operator followed by an expression.
 NUMBER: 3
 DESCRIPTION: 'When' with bound value and 'when condition' with contains operator and object without defined contains operator.
 */

// CASE DESCRIPTION: 'When' with object of custom class, without defined contains operator.
fun case_1(value: Int, value1: _EmptyClass): String {
    when (value) {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value1  -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with object of various basic types (Int, Any, Nothing, Unit, Map — Collection example), without defined contains operator.
fun case_2(
    value: Int,
    value1: Int,
    value2: Any,
    value3: Nothing,
    <!UNUSED_PARAMETER!>value4<!>: Unit,
    <!UNUSED_PARAMETER!>value5<!>: Map<Int, Int>
): String {
    when (value) {
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value1 -> {}
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value2 -> {}
        <!OVERLOAD_RESOLUTION_AMBIGUITY, TYPE_MISMATCH_IN_RANGE, UNREACHABLE_CODE!>in<!> value3 -> <!UNREACHABLE_CODE!>{}<!>
        <!UNREACHABLE_CODE!><!UNRESOLVED_REFERENCE_WRONG_RECEIVER, TYPE_MISMATCH_IN_RANGE!>in<!> value4 -> {}<!>
        <!UNREACHABLE_CODE!>in value5 -> {}<!>
    }

    return ""
}
