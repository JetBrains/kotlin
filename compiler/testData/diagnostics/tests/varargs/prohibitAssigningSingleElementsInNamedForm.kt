// LANGUAGE: +ProhibitAssigningSingleElementsToVarargsInNamedForm +AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
// DIAGNOSTICS: -UNUSED_PARAMETER

annotation class Anno1(vararg val s: String)
annotation class Anno2(vararg val i: Int)

@Anno1(s = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_ANNOTATION_ERROR, TYPE_MISMATCH!>"foo"<!>)
@Anno2(i = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION!>intArrayOf(1)<!>)
fun f1() {}

@Anno1(s = ["foo"])
@Anno2(i = intArrayOf(1))
fun f2() {}

@Anno1(s = arrayOf(elements = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR, TYPE_MISMATCH!>"foo"<!>))
@Anno2(i = intArrayOf(elements = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>intArrayOf(1)<!>))
fun f3() {}

fun foo(vararg ints: Int) {}

fun test() {
    foo(ints = <!ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR, CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    foo(ints = *<!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>intArrayOf(1)<!>)
}
