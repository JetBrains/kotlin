// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7630
 */
fun case_1() {
    var x: Class? = Class()
    if (x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        x++
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7630
 */
fun case_2() {
    var x: Class?
    x = Class()
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    x--
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7630
 */
fun case_3() {
    var x: Class? = Class()
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    --x
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7630
 */
fun case_4() {
    var x: Class? = Class()
    x as Class
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    ++x
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

// TESTCASE NUMBER: 5
fun case_5() {
    var x: Class? = Class()
    x as Class
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    x = x + x
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

// TESTCASE NUMBER: 6
fun case_6() {
    var x: Class? = Class()
    if (x is Class) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
        x = x - x
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7630
 */
fun case_7() {
    var x: Class?
    x = Class()
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    x += x
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}

/*
 * TESTCASE NUMBER: 8
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-7630
 */
fun case_8() {
    var x: Class? = Class()
    x!!
    <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class?")!>x<!>
    x -= x
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class?")!>x<!>.<!UNSAFE_CALL!>equals<!>(10)
}
