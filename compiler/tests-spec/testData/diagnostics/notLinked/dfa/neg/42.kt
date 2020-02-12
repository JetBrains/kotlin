// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 42
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-1982
 */
fun case_1(x: Any) {
    if (x is Int || x is Float) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-1982
 */
fun case_2(x: Any?) {
    if (x is Int || x is Float?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-1982
 */
fun case_3(x: Any?) {
    if (x is Int? || x is Float) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-1982
 */
fun case_4(x: Any?) {
    if (x is Int? || x is Float?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-1982
 */
fun case_5(x: Any?) {
    if (x is Int || x is Float) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 6
 * ISSUES: KT-1982
 */
fun <T> case_6(x: T) {
    if (x is Int || x is Float) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 7
 * ISSUES: KT-1982
 */
fun <T> case_7(x: T) {
    if (x is Int? || x is Float?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 8
 * ISSUES: KT-1982
 */
inline fun <reified T> case_8(x: T) {
    if (x is Int? || x is Float?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 9
 * ISSUES: KT-1982
 */
inline fun <reified T : Any> case_9(x: T) {
    if (x is Int<!USELESS_NULLABLE_CHECK!>?<!> || x is Float<!USELESS_NULLABLE_CHECK!>?<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
    }
}

/*
 * TESTCASE NUMBER: 10
 * ISSUES: KT-1982
 */
inline fun <reified T : Any> case_10(x: T) {
    if (x is ClassLevel2 || x is ClassLevel21 || x is ClassLevel22 || x is ClassLevel23) {
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.<!UNRESOLVED_REFERENCE!>test1<!>()
    }
}

/*
 * TESTCASE NUMBER: 11
 * ISSUES: KT-1982
 */
inline fun <reified T : Any> case_11(x: T) {
    if (x !is ClassLevel2 && x !is ClassLevel21 && x !is ClassLevel22 && x !is ClassLevel23) return
    <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T")!>x<!>.<!UNRESOLVED_REFERENCE!>test1<!>()
}

/*
 * TESTCASE NUMBER: 12
 * ISSUES: KT-1982
 */
fun case_12(x: Any) {
    if (x !is Int && x !is Float) return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
}

/*
 * TESTCASE NUMBER: 13
 * ISSUES: KT-1982
 */
fun <T> case_13(x: T) {
    if (x !is Int && x !is Float) throw Exception()
    <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("T & T!!")!>x<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()
}

/*
 * TESTCASE NUMBER: 14
 * ISSUES: KT-1982
 */
fun case_14(x: Any) {
    if (x is Int || x is Float) {
        if (x is Float) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Float")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Float")!>x<!>.<!UNRESOLVED_REFERENCE!>NaN<!>
        } else {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>x<!>.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
        }
    }
}