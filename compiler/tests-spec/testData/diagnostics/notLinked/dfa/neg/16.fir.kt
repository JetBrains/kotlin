// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_1(x: ClassWithCustomEquals) {
    val y = null
    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: ClassWithCustomEquals) {
    if (x == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_3(x: ClassWithCustomEquals, y: Nothing?) {
    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_4(x: ClassWithCustomEquals) {
    val y = null
    if (y == x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_5(x: ClassWithCustomEquals, y: Nothing?) {
    if (y == x) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_6(x: ClassWithCustomEquals) {
    val y = null
    if (x == y == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: ClassWithCustomEquals) {
    if ((x != null) == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28243
 */
fun case_8(x: ClassWithCustomEquals, y: Nothing?) {
    if (!(y == x) == false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithCustomEquals")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
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
