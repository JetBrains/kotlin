// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 8
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Class?) {
    if (x?.fun_4()?.fun_4()?.fun_4()?.fun_4() != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.fun_4()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.fun_4()<!UNSAFE_CALL!>.<!>fun_4()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.fun_4()<!UNSAFE_CALL!>.<!>fun_4()<!UNSAFE_CALL!>.<!>fun_4()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.fun_4()<!UNSAFE_CALL!>.<!>fun_4()<!UNSAFE_CALL!>.<!>fun_4()<!UNSAFE_CALL!>.<!>fun_4()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Class?) {
    if (x?.fun_4()?.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.fun_4()
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.fun_4()<!UNSAFE_CALL!>.<!>prop_8
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Class?) {
    if (x?.prop_8?.fun_4() != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & Class")!>x<!>.prop_8.fun_4()<!UNSAFE_CALL!>.<!>prop_8
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Class?) {
    if (x?.prop_8.also { } != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>prop_8
        <!DEBUG_INFO_EXPRESSION_TYPE("Class?")!>x<!><!UNSAFE_CALL!>.<!>prop_8<!UNSAFE_CALL!>.<!>fun_4()
    }
}
