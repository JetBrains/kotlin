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
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class"), DEBUG_INFO_EXPRESSION_TYPE("Class"), DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>[if (true) {<!VAL_REASSIGNMENT!>x<!>=null;0} else 0] += <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>[0]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>[0].inv()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: Class? = <!INITIALIZER_TYPE_MISMATCH!>10<!>
    x!!
    x(if (true) {x=null;0} else 0, <!ARGUMENT_TYPE_MISMATCH, DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>fun_1()
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Class? = Class()
    x!!
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>[if (true) {x=null;0} else 0, <!UNSAFE_CALL!><!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>[0]<!>]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>fun_1()
}
