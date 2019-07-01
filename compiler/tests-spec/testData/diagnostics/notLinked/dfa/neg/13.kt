// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 13
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30507
 */

// TESTCASE NUMBER: 1
fun case_1(x: Class?) {
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {<!VAL_REASSIGNMENT!>x<!>=null;0} else 0] += <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[0]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[0].inv()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: Class? = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>10<!>
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>(if (true) {x=null;0} else 0, <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.fun_1()
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Class? = Class()
    x!!
    val <!UNUSED_VARIABLE!>y<!> = <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[if (true) {x=null;0} else 0, <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>[0]]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?"), DEBUG_INFO_SMARTCAST!>x<!>.fun_1()
}