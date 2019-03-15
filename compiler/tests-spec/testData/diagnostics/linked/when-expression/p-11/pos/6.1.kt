// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 6
 * NUMBER: 1
 * DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered.
 * HELPERS: sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass): Int = when (value_1) {
    is SealedChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.number
    is SealedChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.e1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.e2
    is SealedChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2
}

// TESTCASE NUMBER: 2
fun case_2(value_1: SealedClass): String = when (value_1) {
    <!USELESS_IS_CHECK!>is SealedClass<!> -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClassWithMethods): String = when (value_1) {
    is SealedWithMethodsChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1()
    is SealedWithMethodsChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2()
    is SealedWithMethodsChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m3()
}

// TESTCASE NUMBER: 4
fun case_4(value_1: SealedClassWithObjects): String = when (value_1) {
    SealedWithObjectsChild1 -> ""
    SealedWithObjectsChild2 -> ""
    SealedWithObjectsChild3 -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClassMixed): String = when (value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
    SealedMixedChildObject1 -> ""
    SealedMixedChildObject2 -> ""
    SealedMixedChildObject3 -> ""
}

/*
 * TESTCASE NUMBER: 6
 * DISCUSSION: is it correct that objects can be checked using the type checking operator?
 */
fun case_6(value_1: SealedClassMixed): String = when (value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
    is SealedMixedChildObject1 -> ""
    is SealedMixedChildObject2 -> ""
    is SealedMixedChildObject3 -> ""
}

// TESTCASE NUMBER: 7
fun case_7(value_1: SealedClassEmpty): String = when (value_1) {
    else -> ""
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR: must be exhaustive
 * ISSUES: KT-22996
 */
fun case_8(value: SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is SealedChild1, !is SealedChild3?, <!USELESS_IS_CHECK!>is SealedChild3?<!> -> ""
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR: must be exhaustive
 * ISSUES: KT-22996
 */
fun case_9(value: SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is SealedChild1, !is SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is SealedChild3?<!> -> ""
}
