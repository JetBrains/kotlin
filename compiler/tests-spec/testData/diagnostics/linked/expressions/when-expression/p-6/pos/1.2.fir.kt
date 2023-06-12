// LANGUAGE: +WarnAboutNonExhaustiveWhenOnAlgebraicTypes
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: 'When' with bound value and type test condition (invert type checking operator).
 * HELPERS: classes, sealedClasses, objects
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: SealedClass) = when (value_1) {
    !is SealedChild1 -> {}
    !is SealedChild2 -> {}
    !is SealedChild3 -> {}
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_2(value_1: SealedClass?): String = when (value_1) {
    !is SealedChild2 -> "" // including null
    <!USELESS_IS_CHECK!>is SealedChild2<!> -> ""
    <!SENSELESS_NULL_IN_WHEN!>null<!> -> "" // redundant
}

// TESTCASE NUMBER: 3
fun case_3(value_1: SealedClass?): String = when (value_1) {
    !is SealedChild2? -> "" // null isn't included
    is SealedChild2 -> ""
    null -> ""
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_4(value_1: SealedClass?) {
    when (value_1) {
        !is SealedChild2 -> {} // including null
        <!USELESS_IS_CHECK!>is SealedChild2?<!> -> {} // redundant nullable type check
        else -> {}
    }
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any): String {
    when (value_1) {
        is EmptyObject -> return ""
        !is ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}
