// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 41
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
inline fun <reified T> case_1(x: Any?) {
    when (x) {
        is T -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 2
inline fun <reified T> case_2(x: Any?) {
    when (x) {
        is Any -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
        }
        is T -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
        else -> return
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 3
inline fun <reified T> case_3(x: Any?) {
    when (x) {
        is T? -> return
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 4
inline fun <reified T> case_4(x: Any?) {
    when (x) {
        is T -> return
        else -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 5
inline fun <reified T> case_5(x: Any?) {
    if (x is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    } else return
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 6
inline fun <reified T> case_6(x: Any?) {
    if (x is T?) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}

// TESTCASE NUMBER: 7
inline fun <reified T> case_7(x: Any?) {
    if (x is Any) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
    } else if (x is T) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
    } else return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
}