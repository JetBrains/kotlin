// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 11 -> sentence 8
 * NUMBER: 2
 * DESCRIPTION: Checking for not exhaustive 'when' on the nullable sealed classes (and several checks for not sealed).
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    is _SealedChild3 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    _SealedMixedChildObject1 -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null, is _SealedMixedChild1, is _SealedMixedChild2, _SealedMixedChildObject1 -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
    _SealedMixedChildObject1 -> ""
    _SealedMixedChildObject2 -> ""
    _SealedMixedChildObject3 -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2 -> ""
    is _SealedMixedChild3 -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: _SealedClassMixed?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {}

// TESTCASE NUMBER: 7
fun case_7(value_1: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is _SealedMixedChild1 -> ""
    is _SealedMixedChild2-> ""
    is _SealedMixedChild3 -> ""
    null -> ""
}

// TESTCASE NUMBER: 8
fun case_8(value_1: _SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    _SealedMixedChildObject1 -> ""
}

/*
 * TESTCASE NUMBER: 9
 * DISCUSSION: maybe make exhaustive without else?
 */
fun case_9(value_1: Any?): String = <!NO_ELSE_IN_WHEN!>when<!> (value_1) {
    is Any -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 10
 * DISCUSSION
 * ISSUES: KT-26044
 */
fun case_10(value: _SealedClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {}
