// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun test1(x: ClassLevel1?) {
    if (x!! is ClassLevel2) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel1?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1 & ClassLevel1?")!>x<!>.<!UNRESOLVED_REFERENCE!>test2<!>()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?) {
    (x as ClassLevel1?)!!
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>test1<!>()
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x as ClassLevel1? is ClassLevel1) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!>.<!UNSAFE_CALL!>test1<!>()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if ((x as Class).prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Class) {
    if (x!!.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & Class")!>x<!>.prop_8.<!UNSAFE_CALL!>prop_8<!>
    }
}
