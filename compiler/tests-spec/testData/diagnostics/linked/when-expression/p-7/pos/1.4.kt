// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 4
 DESCRIPTION: 'When' with bound value and enumaration of type test conditions (with invert type checking operator).
 */

// CASE DESCRIPTION: 'When' with direct and invert type checking operator in the one branch and other branch.
fun case_1(value: _SealedClass): String = when (value) {
    is _SealedChild1, !is _SealedChild3 -> ""
    <!USELESS_IS_CHECK!>is _SealedChild3<!> -> ""
}

// CASE DESCRIPTION: 'When' with three invert type checking operator in the one branch.
fun case_2(value: _SealedClass) = when (value) {
    !is _SealedChild1, !is _SealedChild2, !is _SealedChild3 -> {}
}

// CASE DESCRIPTION: 'When' with direct (first) and invert (second) type checking operator on the some type in the one branch.
fun case_3(value: _SealedClass): String = when (value) {
    is _SealedChild2, !is _SealedChild2 -> ""
}

// CASE DESCRIPTION: 'When' with direct (second) and invert (first) type checking operator in the one branch.
fun case_4(value: _SealedClass): String = when (value) {
    !is _SealedChild1, <!USELESS_IS_CHECK!>is _SealedChild1<!> -> ""
}

// CASE DESCRIPTION: 'When' with direct and invert (nullable) type checking operator on the some type in the one branch.
fun case_5(value: Any?): String = when (value) {
    is _SealedChild3, !is _SealedChild3? -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with direct and invert type checking operator in the one branch and other branch and double nullable type check.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_6(value: Any?): String = when (value) {
    is Boolean?, !is _SealedChild3 -> "" // double nullable type check in the one branch
    <!USELESS_IS_CHECK!>is _SealedChild3<!> -> ""
    else -> ""
}

/*
 CASE DESCRIPTION: 'When' with direct and invert type checking operator and null-check in the one branch.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_7(value: Any?): String = when (value) {
    is Number?, null, !is _SealedChild3 -> "" // triple nullable type check in the one branch
    else -> ""
}
