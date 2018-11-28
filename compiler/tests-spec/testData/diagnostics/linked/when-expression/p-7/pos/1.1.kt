// !WITH_CLASSES
// !WITH_OBJECTS

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [1] Type test condition: type checking operator followed by type.
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and type test condition.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Any): String {
    when (value_1) {
        is Int -> return ""
        is Float -> return ""
        is Double -> return ""
        is String -> return ""
        is Char -> return ""
        is Boolean -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any?): String = when (value_1) {
    is Int? -> "" // if value is null then this branch will be executed
    is Float -> ""
    else -> ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Any?): String = when (value_1) {
    is Any -> ""
    else -> ""
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Any): String = when (value_1) {
    <!USELESS_IS_CHECK!>is Any<!USELESS_NULLABLE_CHECK!>?<!><!> -> ""
    else -> ""
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-22996
 */
fun case_5(value_1: Any?): String = when (value_1) {
    is Double -> ""
    is Int? -> "" // if value is null then this branch will be executed
    is String -> ""
    is Float? -> "" // redundant nullable type check
    is Char -> ""
    is Boolean -> ""
    else -> ""
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Any): String {
    when (value_1) {
        is _EmptyObject -> return ""
        is _ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}
