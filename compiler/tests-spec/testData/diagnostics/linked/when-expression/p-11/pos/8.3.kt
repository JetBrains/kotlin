// !WITH_SEALED_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 3
 * DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered and contains a null check.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _SealedClass?): Int = when (value_1) {
    is _SealedChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.number
    is _SealedChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.e1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.e2
    is _SealedChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2
    null -> 0
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _SealedClass?): String = when (value_1) {
    is _SealedClass -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _SealedClassWithMethods?): String = when (value_1) {
    is _SealedWithMethodsChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1()
    is _SealedWithMethodsChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2()
    is _SealedWithMethodsChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m3()
    null -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: _SealedClassWithObjects?): String = when (value_1) {
    _SealedWithObjectsChild1 -> ""
    _SealedWithObjectsChild2 -> ""
    _SealedWithObjectsChild3 -> ""
    null -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: _SealedClassMixed?): String = when (value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    _SealedMixedChildObject1 -> ""
    _SealedMixedChildObject2 -> ""
    _SealedMixedChildObject3 -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 6
 * DISCUSSION: is it correct that objects can be checked using the type checking operator?
 */
fun case_6(value_1: _SealedClassMixed?): String = when (value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    is _SealedMixedChildObject1 -> ""
    is _SealedMixedChildObject2 -> ""
    is _SealedMixedChildObject3 -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR: must be exhaustive
 * ISSUES: KT-26044
 */
fun case_7(value: _SealedClassEmpty?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    null -> ""
}
