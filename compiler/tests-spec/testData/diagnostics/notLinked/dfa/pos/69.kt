// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 69
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29083
 */

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
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>test1()
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x as ClassLevel1? is ClassLevel1) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel1? & kotlin.Any?")!>x<!><!UNSAFE_CALL!>.<!>test1()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if ((x as Class).prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.prop_8<!UNSAFE_CALL!>.<!>prop_8
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Class) {
    if (x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.prop_8 != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class")!>x<!>.prop_8<!UNSAFE_CALL!>.<!>prop_8
    }
}
