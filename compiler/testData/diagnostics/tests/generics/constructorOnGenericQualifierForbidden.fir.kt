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
    n2: Generic<!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!><<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>_<!>><!>.Nested,
    n3: Generic.Nested,
    n4: TA.<!UNRESOLVED_REFERENCE!>Nested<!>,
    i: Generic<Int>.Inner,
    i2: Generic<<!UNRESOLVED_REFERENCE!>_<!>>.Inner,
    i3: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>Generic<!>.Inner,
    i4: TA.<!UNRESOLVED_REFERENCE!>Nested<!>,
) {
    Generic<!GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL_ERROR!><Int><!>.Nested()
    Generic<!GENERIC_QUALIFIER_ON_CONSTRUCTOR_CALL_ERROR!><_><!>.Nested()
    Generic.Nested()
    TA.<!UNRESOLVED_REFERENCE!>Nested<!>()

    Generic<String>.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    Generic<_>.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    Generic.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>Inner<!>()
    TA.<!UNRESOLVED_REFERENCE!>Inner<!>()

    Generic<Boolean>().Inner()
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Generic<!><<!CANNOT_INFER_PARAMETER_TYPE!>_<!>>().Inner()
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Generic<!>().Inner()
    TA().Inner()
}
