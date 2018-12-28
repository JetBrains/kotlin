// !WITH_SEALED_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: 'When' with bound value and enumaration of type test conditions (with invert type checking operator).
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _SealedClass): String = when (value_1) {
    is _SealedChild1, !is _SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is _SealedChild3<!> -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: _SealedClass) = when (value_1) {
    !is _SealedChild1, !is _SealedChild2, !is _SealedChild3 -> {}
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _SealedClass): String = when (value_1) {
    is _SealedChild2, !is _SealedChild2 -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: _SealedClass): String = when (value_1) {
    !is _SealedChild1, <!USELESS_IS_CHECK!>is _SealedChild1<!> -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any?): String = when (value_1) {
    is _SealedChild3, !is _SealedChild3? -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_6(value_1: Any?): String = when (value_1) {
    is Boolean?, !is _SealedChild3 -> "" // double nullable type check in the one branch
    <!USELESS_IS_CHECK!>is _SealedChild3<!> -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_7(value_1: Any?): String = when (value_1) {
    is Number?, null, !is _SealedChild3 -> "" // triple nullable type check in the one branch
    else -> ""
}
