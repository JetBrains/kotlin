// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 3
 * DESCRIPTION: 'When' with bound value and enumaration of type test conditions.
 * HELPERS: classes, sealedClasses, objects
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Any) = when (value_1) {
    is Int -> {}
    is Float, is Char, is Boolean -> {}
    is String -> {}
    else -> {}
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any?) = when (value_1) {
    is Float, is Char, is SealedClass? -> "" // if value is null then this branch will be executed
    is Double, is Boolean, is ClassWithCompanionObject.Companion -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_3(value_1: Any?) = when (value_1) {
    is Float, is Char, is Int? -> "" // if value is null then this branch will be executed
    is SealedChild2, is Boolean?, is String -> "" // redundant nullable type check
    else -> ""
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_4(value_1: Any?) = when (value_1) {
    is Float, is Char?, is Int? -> "" // double nullable type check in the one branch
    is SealedChild1, is Boolean, is String -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_5(value_1: Any?): String {
    when (value_1) {
        is Float, is Char?, is Int -> return ""
        is Double, is EmptyObject, is String -> return ""
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> return "" // null-check redundant
        else -> return ""
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_6(value_1: Any?): String {
    when (value_1) {
        is Float, is Char?, <!SENSELESS_NULL_IN_WHEN!>null<!>, is Int -> return "" // double nullable type check in the one branch
        is Double, is EmptyObject, is String -> return ""
        else -> return ""
    }
}
