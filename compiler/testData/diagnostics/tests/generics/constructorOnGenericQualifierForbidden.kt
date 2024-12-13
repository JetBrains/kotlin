// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73527
// LANGUAGE: +ProhibitGenericQualifiersOnConstructorCalls

class Generic<T> {
    class Nested
    inner class Inner
}

typealias TA = Generic<Double>

fun test(
    n: Generic<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><String><!>.Nested,
    n2: Generic<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><<!UNRESOLVED_REFERENCE!>_<!>><!>.Nested,
    n3: Generic.Nested,
    n4: TA.<!UNRESOLVED_REFERENCE!>Nested<!>,
    i: Generic<Int>.Inner,
    i2: Generic<<!UNRESOLVED_REFERENCE!>_<!>>.Inner,
    i3: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!>.Inner,
    i4: TA.<!UNRESOLVED_REFERENCE!>Nested<!>,
) {
    <!FUNCTION_CALL_EXPECTED!>Generic<Int><!>.<!UNRESOLVED_REFERENCE!>Nested<!>()
    <!FUNCTION_CALL_EXPECTED!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Generic<!><_><!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Nested<!>()
    Generic.Nested()
    TA.<!UNRESOLVED_REFERENCE!>Nested<!>()

    <!FUNCTION_CALL_EXPECTED!>Generic<String><!>.Inner()
    <!FUNCTION_CALL_EXPECTED!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Generic<!><_><!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>Inner<!>()
    Generic.<!RESOLUTION_TO_CLASSIFIER!>Inner<!>()
    TA.<!UNRESOLVED_REFERENCE!>Inner<!>()

    Generic<Boolean>().Inner()
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Generic<!><_>().<!DEBUG_INFO_MISSING_UNRESOLVED!>Inner<!>()
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Generic<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>Inner<!>()
    TA().Inner()
}
