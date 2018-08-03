// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 9: The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 2
 DESCRIPTION: Checking for not exhaustive when when covered by all possible subtypes, but no null check (or with no null check, but not covered by all possible subtypes).
 */

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Sealed class with null-check branch, but all possible subtypes not covered.
fun case_1(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    _SealedMixedChildObject1 -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Sealed class without null-check branch.
fun case_2(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    _SealedMixedChildObject1 -> ""
    _SealedMixedChildObject2 -> ""
    _SealedMixedChildObject3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Sealed class without null-check branch and all possible subtypes not covered.
fun case_3(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Sealed class without branches.
fun case_4(value: _SealedClassMixed?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) {}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Sealed class with null-check branch, but object not covered.
fun case_5(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2-> ""
    is _SealedMixedChild3 -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the Sealed class without null-check branch and only object covered.
fun case_6(value: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    _SealedMixedChildObject1 -> ""
}
