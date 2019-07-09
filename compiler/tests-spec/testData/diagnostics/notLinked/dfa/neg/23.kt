// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 23
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-28965
 */
inline fun <reified T, reified K> case_1(x: T) {
    if (x is K) {
        <!DEBUG_INFO_EXPRESSION_TYPE("K & T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K & T")!>x<!><!UNSAFE_CALL!>.<!>equals(x)
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-28965
 */
inline fun <reified T, reified K> case_2(x: T) {
    x as K
    <!DEBUG_INFO_EXPRESSION_TYPE("K & T")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("K & T")!>x<!><!UNSAFE_CALL!>.<!>equals(x)
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-28965
 */
inline fun <reified T, reified K> case_3() {
    var x: T? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>10 as T<!>
    x = null
    if (<!DEBUG_INFO_CONSTANT!>x<!> is K) {
        <!DEBUG_INFO_EXPRESSION_TYPE("K & T? & kotlin.Nothing?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("K & T? & kotlin.Nothing?")!>x<!>
        println(1)
    }
}

// TESTCASE NUMBER: 4
inline fun <reified T, reified K> case_4(x: T?) {
    if (x is K) {
        <!DEBUG_INFO_EXPRESSION_TYPE("K & T?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K & T?")!>x<!><!UNSAFE_CALL!>.<!>equals(x)
    }
}

// TESTCASE NUMBER: 5
inline fun <reified T, reified K> case_5(x: T) {
    if (x is K?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("K? & T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("K? & T")!>x<!><!UNSAFE_CALL!>.<!>equals(x)
    }
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-28965
 */
inline fun <reified T, reified K> case_6() {
    var x: T? = 10 as T
    if (x is K) {
        x = null
        <!DEBUG_INFO_EXPRESSION_TYPE("T? & kotlin.Nothing?")!>x<!><!UNSAFE_CALL!>.<!>equals(10)
        <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("T? & kotlin.Nothing?")!>x<!>
        println(1)
    }
}
