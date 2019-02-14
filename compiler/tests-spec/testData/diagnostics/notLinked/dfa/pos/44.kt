// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 44
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28760
 */
fun Int?.case_1() {
    val x = this
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>this<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>this<!><!UNSAFE_CALL!>.<!>inv()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28760
 */
fun Int?.case_2() {
    val x = this
    if (this != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>inv()
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28760
 */
fun Int?.case_3() {
    val x = this
    this!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>inv()
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28760
 */
fun Int?.case_4() {
    val x = this
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>this<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>this<!><!UNSAFE_CALL!>.<!>inv()
}
