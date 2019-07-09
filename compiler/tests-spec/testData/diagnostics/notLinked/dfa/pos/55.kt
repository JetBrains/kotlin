// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 55
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-10662
 */
fun case_1(x: Any) {
    if (x is String) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!><!DEBUG_INFO_SMARTCAST!>x<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.String")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-10662
 */
fun case_2(x: Any?) {
    if (x is String?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!><!DEBUG_INFO_SMARTCAST!>x<!>!!<!>.length
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-10662
 */
fun case_3(x: Any) {
    if (x is Map.Entry<*, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map.Entry<kotlin.Any?, kotlin.Any?>")!><!DEBUG_INFO_SMARTCAST!>x<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>.key
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.collections.Map.Entry<*, *>")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-10662
 */
fun case_4(x: Any?) {
    if (x is Map.Entry<*, *>?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.Map.Entry<kotlin.Any?, kotlin.Any?>")!><!DEBUG_INFO_SMARTCAST!>x<!>!!<!>.value
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.collections.Map.Entry<*, *>?")!>x<!>
    }
}