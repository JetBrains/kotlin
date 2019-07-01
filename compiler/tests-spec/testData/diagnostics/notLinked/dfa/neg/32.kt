// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 32
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1, 2, 3, 4, 5
fun stringArg(number: String) {}

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_1(x: Int?) {
    if (x == null) {
        <!UNREACHABLE_CODE!>stringArg(<!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!><!ALWAYS_NULL!>x<!>!!<!><!UNREACHABLE_CODE!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing"), UNREACHABLE_CODE!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_2(x: Int?, y: Nothing?) {
    if (x == <!DEBUG_INFO_CONSTANT!>y<!>) {
        <!UNREACHABLE_CODE!>stringArg(<!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!><!ALWAYS_NULL!>x<!>!!<!><!UNREACHABLE_CODE!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing"), UNREACHABLE_CODE!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_3(x: Int?) {
    if (x == null) {
        <!ALWAYS_NULL!>x<!> as Int
        <!UNREACHABLE_CODE!>stringArg(<!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!><!UNREACHABLE_CODE!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing"), UNREACHABLE_CODE!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_4(x: Int?) {
    if (x == null) {
        <!ALWAYS_NULL!>x<!>!!
        <!UNREACHABLE_CODE!>stringArg(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing"), UNREACHABLE_CODE!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_5(x: Int?) {
    if (x == null) {
        var y = <!DEBUG_INFO_CONSTANT!>x<!>
        <!ALWAYS_NULL!>y<!>!!
        <!UNREACHABLE_CODE!>stringArg(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>y<!>)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing"), UNREACHABLE_CODE!>y<!>
    }
}
