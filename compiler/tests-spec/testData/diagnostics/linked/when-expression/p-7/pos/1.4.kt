// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTIONS: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 4
 DESCRIPTION: 'When' with bound value and enumaration of type test conditions (with invert type checking operator).
 */

// CASE DESCRIPTION: 'When' with direct and invert type checking operator in the one branch and other branch.
fun case_1(value_1: _SealedClass): String = when (value_1) {
    is _SealedChild1, !is _SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is _SealedChild3<!> -> ""
}

// CASE DESCRIPTION: 'When' with three invert type checking operator in the one branch.
fun case_2(value_1: _SealedClass) = when (value_1) {
    !is _SealedChild1, !is _SealedChild2, !is _SealedChild3 -> {}
}

// CASE DESCRIPTION: 'When' with direct (first) and invert (second) type checking operator on the some type in the one branch.
fun case_3(value_1: _SealedClass): String = when (value_1) {
    is _SealedChild2, !is _SealedChild2 -> ""
}

// CASE DESCRIPTION: 'When' with direct (second) and invert (first) type checking operator in the one branch.
fun case_4(value_1: _SealedClass): String = when (value_1) {
    !is _SealedChild1, <!USELESS_IS_CHECK!>is _SealedChild1<!> -> ""
}

// CASE DESCRIPTION: 'When' with direct and invert (nullable) type checking operator on the some type in the one branch.
fun case_5(value_1: Any?): String = when (value_1) {
    is _SealedChild3, !is _SealedChild3? -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with direct and invert type checking operator in the one branch and other branch and double nullable type check.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_6(value_1: Any?): String = when (value_1) {
    is Boolean?, !is _SealedChild3 -> "" // double nullable type check in the one branch
    <!USELESS_IS_CHECK!>is _SealedChild3<!> -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with direct and invert type checking operator and null-check in the one branch.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_7(value_1: Any?): String = when (value_1) {
    is Number?, null, !is _SealedChild3 -> "" // triple nullable type check in the one branch
    else -> ""
}
