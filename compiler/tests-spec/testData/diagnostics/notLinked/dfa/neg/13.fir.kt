// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(x: Class?) {
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>[if (true) {x=null;0} else 0] += <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>[0]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>[0].inv()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: Class? = 10
    x!!
    <!NONE_APPLICABLE!>x<!>(if (true) {x=null;0} else 0, <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>fun_1<!>()
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Class? = Class()
    x!!
    val y = <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>[if (true) {x=null;0} else 0, <!INAPPLICABLE_CANDIDATE!><!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>[0]<!>]
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>fun_1<!>()
}
