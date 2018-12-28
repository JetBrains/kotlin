// !WITH_SEALED_CLASSES
// !WITH_CLASSES
// !WITH_OBJECTS

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-draft
 * PLACE: when-expression -> paragraph 7 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and type test condition (invert type checking operator).
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: _SealedClass) = when (value_1) {
    !is _SealedChild1 -> {}
    !is _SealedChild2 -> {}
    !is _SealedChild3 -> {}
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_2(value_1: _SealedClass?): String = when (value_1) {
    !is _SealedChild2 -> "" // including null
    <!USELESS_IS_CHECK!>is _SealedChild2<!> -> ""
    null -> "" // redundant
}

// TESTCASE NUMBER: 3
fun case_3(value_1: _SealedClass?): String = when (value_1) {
    !is _SealedChild2? -> "" // null isn't included
    is _SealedChild2 -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_4(value_1: _SealedClass?) {
    when (value_1) {
        !is _SealedChild2 -> {} // including null
        <!USELESS_IS_CHECK!>is _SealedChild2?<!> -> {} // redundant nullable type check
    }
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any): String {
    when (value_1) {
        is _EmptyObject -> return ""
        !is _ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}
