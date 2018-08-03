// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !CHECK_TYPE
// !WITH_SEALED_CLASSES

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 7: The bound expression is of a sealed class type and all its possible subtypes are covered using type test conditions of this expression;
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when when not covered by all possible subtypes.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking and equality with object).
fun case_1(value: _SealedClassMixed): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    _SealedMixedChildObject1 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking).
fun case_2(value: _SealedClassMixed): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class with several subtypes (no branches).
fun case_3(value: _SealedClassMixed): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class with one subtype (no branches).
fun case_4(value: _SealedClassSingleWithObject): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty sealed class (without subtypes).
fun case_5(value: _SealedClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value) { }