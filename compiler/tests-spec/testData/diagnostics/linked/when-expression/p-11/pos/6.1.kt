// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [6] The bound valueession is of a sealed class type and all its possible subtypes are covered using type test conditions of this valueession;
 NUMBER: 1
 DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered.
 */

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes covered).
fun case_1(value: _SealedClass): Int = when (value) {
    is _SealedChild1 -> <!DEBUG_INFO_SMARTCAST!>value<!>.number
    is _SealedChild2 -> <!DEBUG_INFO_SMARTCAST!>value<!>.e1 + <!DEBUG_INFO_SMARTCAST!>value<!>.e2
    is _SealedChild3 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m1 + <!DEBUG_INFO_SMARTCAST!>value<!>.m2
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single sealed class subtypes covered).
fun case_2(value: _SealedClass): String = when (value) {
    <!USELESS_IS_CHECK!>is _SealedClass<!> -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes with methods covered).
fun case_3(value: _SealedClassWithMethods): String = when (value) {
    is _SealedWithMethodsChild1 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m1()
    is _SealedWithMethodsChild2 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m2()
    is _SealedWithMethodsChild3 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m3()
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all objects covered using implicit equality operator).
fun case_4(value: _SealedClassWithObjects): String = when (value) {
    _SealedWithObjectsChild1 -> ""
    _SealedWithObjectsChild2 -> ""
    _SealedWithObjectsChild3 -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all subtypes and objects covered).
fun case_5(value: _SealedClassMixed): String = when (value) {
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
fun case_6(value: _SealedClassMixed): String = when (value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    is _SealedMixedChildObject1 -> ""
    is _SealedMixedChildObject2 -> ""
    is _SealedMixedChildObject3 -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' on the empty sealed class (without subtypes).
fun case_7(value: _SealedClassEmpty): String = when (value) {
    else -> ""
}
