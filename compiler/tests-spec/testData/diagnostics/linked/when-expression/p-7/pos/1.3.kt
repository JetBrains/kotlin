// !WITH_CLASSES
// !WITH_SEALED_CLASSES
// !WITH_OBJECTS

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [1] Type test condition: type checking operator followed by type.
 * NUMBER: 3
 * DESCRIPTION: 'When' with bound value and enumaration of type test conditions.
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
    is Float, is Char, is _SealedClass? -> "" // if value is null then this branch will be executed
    is Double, is Boolean, is _ClassWithCompanionObject.Companion -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_3(value_1: Any?) = when (value_1) {
    is Float, is Char, is Int? -> "" // if value is null then this branch will be executed
    is _SealedChild2, is Boolean?, is String -> "" // redundant nullable type check
    else -> ""
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_4(value_1: Any?) = when (value_1) {
    is Float, is Char?, is Int? -> "" // double nullable type check in the one branch
    is _SealedChild1, is Boolean, is String -> ""
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
        is Double, is _EmptyObject, is String -> return ""
        null -> return "" // null-check redundant
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
        is Float, is Char?, null, is Int -> return "" // double nullable type check in the one branch
        is Double, is _EmptyObject, is String -> return ""
        else -> return ""
    }
}
