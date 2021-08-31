// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-435
 * MAIN LINK: expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 11
 * NUMBER: 2
 * DESCRIPTION: Non-exhaustive when using subclasses of the nullable sealed class.
 * HELPERS: sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is SealedChild1 -> ""
    is SealedChild2 -> ""
    is SealedChild3 -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    SealedMixedChildObject1 -> ""
    null -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    null, is SealedMixedChild1, is SealedMixedChild2, SealedMixedChildObject1 -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
    SealedMixedChildObject1 -> ""
    SealedMixedChildObject2 -> ""
    SealedMixedChildObject3 -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2 -> ""
    is SealedMixedChild3 -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: SealedClassMixed?): Int = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {}

// TESTCASE NUMBER: 7
fun case_7(value_1: SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    is SealedMixedChild1 -> ""
    is SealedMixedChild2-> ""
    is SealedMixedChild3 -> ""
    null -> ""
}

// TESTCASE NUMBER: 8
fun case_8(value_1: SealedClassMixed?): String = <!NO_ELSE_IN_WHEN!>when<!>(value_1) {
    SealedMixedChildObject1 -> ""
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
fun case_10(value: SealedClassEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value) {}
