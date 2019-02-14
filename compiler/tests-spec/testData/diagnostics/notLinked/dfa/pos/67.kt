// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 67
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    when (x) {
        is Any -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
}

// TESTCASE NUMBER: 2
inline fun <reified T : Any> case_2(x: Any?) {
    when (x) {
        is T -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
}

// TESTCASE NUMBER: 3
inline fun <K : Any, reified T : K> case_3(x: Any?) {
    if (x is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    } else throw Exception()

    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
}