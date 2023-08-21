// !DIAGNOSTICS: -UNUSED_EXPRESSION
// COMPARE_WITH_LIGHT_TREE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 68
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29083
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x!! is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?) {
    (x as Nothing?)!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing")!>x<!>.<!MISSING_DEPENDENCY_CLASS!>inv<!>()
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x as Number? is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Number")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if (x as Class? is Class) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Class")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & Class")!>x<!>.prop_1
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    if (x as Nothing? is Nothing) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing")!>x<!>.<!MISSING_DEPENDENCY_CLASS!>inv<!>()
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    (x as String?)!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>.length
}

// TESTCASE NUMBER: 7
fun case_7(x: Any?) {
    if (x as String? != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.String")!>x<!>.length
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (x as String? == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!><!UNSAFE_CALL!>.<!>length
    }
}
