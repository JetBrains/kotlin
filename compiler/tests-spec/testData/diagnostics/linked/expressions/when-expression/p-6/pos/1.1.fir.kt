// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-296
 * MAIN LINK: expressions, when-expression -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and type test condition.
 * HELPERS: classes, objects
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
    <!USELESS_IS_CHECK!>is Any?<!> -> ""
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
        is EmptyObject -> return ""
        is ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}
