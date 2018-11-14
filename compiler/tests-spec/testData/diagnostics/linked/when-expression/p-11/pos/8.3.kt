// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [8] The bound expression is of a nullable type and one of the areas above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 3
 DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered and contains a null check.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes and null value covered).
fun case_1(value_1: _SealedClass?): Int = when (value_1) {
    is _SealedChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.number
    is _SealedChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.e1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.e2
    is _SealedChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1 + <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2
    null -> 0
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (sealed class itself and null value covered).
fun case_2(value_1: _SealedClass?): String = when (value_1) {
    is _SealedClass -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class with methods subtypes and null value covered).
fun case_3(value_1: _SealedClassWithMethods?): String = when (value_1) {
    is _SealedWithMethodsChild1 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m1()
    is _SealedWithMethodsChild2 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m2()
    is _SealedWithMethodsChild3 -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.m3()
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all objects covered using implicit equality operator and null value covered).
fun case_4(value_1: _SealedClassWithObjects?): String = when (value_1) {
    _SealedWithObjectsChild1 -> ""
    _SealedWithObjectsChild2 -> ""
    _SealedWithObjectsChild3 -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all subtypes and objects covered + null value covered).
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
 CASE DESCRIPTION: Checking for exhaustive 'when' (all subtypes and objects (using type checking operator) covered + null value covered).
 DISCUSSION: is it correct that objects can be checked using the type checking operator?
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
 CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty nullable sealed class (without subtypes).
 UNEXPECTED BEHAVIOUR: must be exhaustive
 ISSUES: KT-26044
 */
fun case_7(value: _SealedClassEmpty?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    null -> ""
}
