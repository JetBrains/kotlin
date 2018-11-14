// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 11
 SENTENCE: [6] The bound expression is of a sealed class type and all its possible subtypes are covered using type test conditions of this expression;
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive 'when' when not covered by all possible subtypes or 'when' does not have bound value.
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking).
fun case_1(value_1: _SealedClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking with enumeration).
fun case_2(value_1: _SealedClass): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedChild1, is _SealedChild2 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking and equality with object).
fun case_3(value_1: _SealedClassMixed): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    _SealedMixedChildObject1 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking and equality with object with enumeration).
fun case_4(value_1: _SealedClassMixed): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    _SealedMixedChildObject1, is _SealedMixedChild2, is _SealedMixedChild1 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking).
fun case_5(value_1: _SealedClassMixed): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class with several subtypes (no branches).
fun case_6(value_1: _SealedClassMixed): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class with one subtype (no branches).
fun case_7(value_1: _SealedClassSingleWithObject): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty sealed class (without subtypes).
fun case_8(value_1: _SealedClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the not sealed class.
fun case_9(value_1: Number): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    is Byte -> ""
    is Double -> ""
    is Float -> ""
    is Int -> ""
    is Long -> ""
    is Short -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on the Any.
 DISCUSSION: maybe make exhaustive without else?
 */
fun case_10(value_1: Any): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    <!USELESS_IS_CHECK!>is Any<!> -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' without bound value on the Sealed class with all subtypes covered.
fun case_11(value_1: _SealedClass): String = <!NO_ELSE_IN_WHEN!>when<!> {
    value_1 is _SealedChild1 -> ""
    value_1 is _SealedChild2 -> ""
    value_1 is _SealedChild3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking).
fun case_12(value_1: _SealedClassMixed): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
}
