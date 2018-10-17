// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [6] The bound valueession is of a sealed class type and all its possible subtypes are covered using type test conditions of this valueession;
 NUMBER: 1
 DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes covered).
fun case_1(value_1: _SealedClass): Int = when (value_1) {
    is _SealedChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.number
    is _SealedChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.e1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.e2
    is _SealedChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single sealed class subtypes covered).
fun case_2(value_1: _SealedClass): String = when (value_1) {
    <!USELESS_IS_CHECK!>is _SealedClass<!> -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes with methods covered).
fun case_3(value_1: _SealedClassWithMethods): String = when (value_1) {
    is _SealedWithMethodsChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1()
    is _SealedWithMethodsChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2()
    is _SealedWithMethodsChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m3()
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all objects covered using implicit equality operator).
fun case_4(value_1: _SealedClassWithObjects): String = when (value_1) {
    _SealedWithObjectsChild1 -> ""
    _SealedWithObjectsChild2 -> ""
    _SealedWithObjectsChild3 -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all subtypes and objects covered).
fun case_5(value_1: _SealedClassMixed): String = when (value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    _SealedMixedChildObject1 -> ""
    _SealedMixedChildObject2 -> ""
    _SealedMixedChildObject3 -> ""
}

/*
 CASE DESCRIPTION: Checking for exhaustive 'when' (all subtypes and objects (using type checking operator) covered).
 DISCUSSION: is it correct that objects can be checked using the type checking operator?
 */
fun case_6(value_1: _SealedClassMixed): String = when (value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    is _SealedMixedChildObject1 -> ""
    is _SealedMixedChildObject2 -> ""
    is _SealedMixedChildObject3 -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' on the empty sealed class (without subtypes).
fun case_7(value_1: _SealedClassEmpty): String = when (value_1) {
    else -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on opposite types.
 UNEXPECTED BEHAVIOUR: must be exhaustive
 ISSUES: KT-22996
 */
fun case_8(value: _SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is _SealedChild1, !is _SealedChild3?, <!USELESS_IS_CHECK!>is _SealedChild3?<!> -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on opposite types.
 UNEXPECTED BEHAVIOUR: must be exhaustive
 ISSUES: KT-22996
 */
fun case_9(value: _SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is _SealedChild1, !is _SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is _SealedChild3?<!> -> ""
}
