// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 16
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_1(x: ClassWithCustomEquals) {
    val y = null
    if (<!SENSELESS_COMPARISON!>x == y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: ClassWithCustomEquals) {
    if (<!SENSELESS_COMPARISON!>x == null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_3(x: ClassWithCustomEquals, y: Nothing?) {
    if (<!SENSELESS_COMPARISON!>x == y<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_4(x: ClassWithCustomEquals) {
    val y = null
    if (<!SENSELESS_COMPARISON!>y == x<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_5(x: ClassWithCustomEquals, y: Nothing?) {
    if (<!SENSELESS_COMPARISON!>y == x<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_6(x: ClassWithCustomEquals) {
    val y = null
    if (<!SENSELESS_COMPARISON!>x == y<!> == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: ClassWithCustomEquals) {
    if ((<!SENSELESS_COMPARISON!>x != null<!>) == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_8(x: ClassWithCustomEquals, y: Nothing?) {
    if (!(<!SENSELESS_COMPARISON!>y == x<!>) == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals & kotlin.Nothing")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_9(x: ClassWithCustomEquals?, y: Interface1) {
    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals? & ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals? & ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>itest1<!>()
    }
}
