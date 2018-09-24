// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [8] The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 2
 DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed classes (and several checks for not sealed).
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class without null-check branch.
fun case_1(value: _SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    is _SealedChild3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class with mixed checks (type and object check) and null-check branch.
fun case_2(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    _SealedMixedChildObject1 -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class with enumeration mixed checks (type and object check) and null-check branch.
fun case_3(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    null, is _SealedMixedChild1, is _SealedMixedChild2, _SealedMixedChildObject1 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class with all subtypes and objects covered without null-check branch.
fun case_4(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    _SealedMixedChildObject1 -> ""
    _SealedMixedChildObject2 -> ""
    _SealedMixedChildObject3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class without null-check branch and all subtypes covered, but objects not covered.
fun case_5(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class without branches.
fun case_6(value: _SealedClassMixed?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) {}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class with null-check branch and all subtypes covered, but objects not covered.
fun case_7(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2-> ""
    is _SealedMixedChild3 -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed class without null-check branch and only object covered.
fun case_8(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _SealedMixedChildObject1 -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty nullable sealed class (without subtypes).
 UNEXPECTED BEHAVIOUR: must be exhaustive
 ISSUES: KT-26044
 */
fun case_9(value: _SealedClassEmpty?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    null -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on the nullable Any.
 DISCUSSION: maybe make exhaustive without else?
 */
fun case_10(value: Any?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is Any -> ""
    null -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on opposite types.
 UNEXPECTED BEHAVIOUR: must be exhaustive
 ISSUES: KT-22996
 */
fun case_11(value: _SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is _SealedChild1, !is _SealedChild3?, <!USELESS_IS_CHECK!>is _SealedChild3?<!> -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on opposite types.
 UNEXPECTED BEHAVIOUR: must be exhaustive
 ISSUES: KT-22996
 */
fun case_12(value: _SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {
    is _SealedChild1, !is _SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is _SealedChild3?<!> -> ""
}

/*
 CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty sealed class (without subtypes).
 DISCUSSION
 ISSUES: KT-26044
 */
fun case_13(value: _SealedClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {}
