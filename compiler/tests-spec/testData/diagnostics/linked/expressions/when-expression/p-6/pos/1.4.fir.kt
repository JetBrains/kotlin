// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: 'When' with bound value and enumaration of type test conditions (with invert type checking operator).
 * HELPERS: sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass): String = when (value_1) {
    is SealedChild1, !is SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is SealedChild3<!> -> ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: SealedClass) = when (value_1) {
    !is SealedChild1, !is SealedChild2, !is SealedChild3 -> {}
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClass): String = when (value_1) {
    is SealedChild2, !is SealedChild2 -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: SealedClass): String = when (value_1) {
    !is SealedChild1, <!USELESS_IS_CHECK!>is SealedChild1<!> -> ""
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any?): String = when (value_1) {
    is SealedChild3, !is SealedChild3? -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_6(value_1: Any?): String = when (value_1) {
    is Boolean?, !is SealedChild3 -> "" // double nullable type check in the one branch
    <!USELESS_IS_CHECK!>is SealedChild3<!> -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_7(value_1: Any?): String = when (value_1) {
    is Number?, <!SENSELESS_NULL_IN_WHEN!>null<!>, !is SealedChild3 -> "" // triple nullable type check in the one branch
    else -> ""
}
