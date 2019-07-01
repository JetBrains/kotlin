// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 2
 * DESCRIPTION: Raw data flow analysis test
 */

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x is Nothing) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Any) {
    if (x is Nothing) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x !is Nothing) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any) {
    if (x !is Nothing) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    if (!(x !is Nothing?)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>?.<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    if (!(x !is Nothing)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 7
fun case_7(x: Any) {
    if (!(x is Nothing)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (!(x is Nothing?)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>?.<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()
    }
}

// TESTCASE NUMBER: 9
fun case_9(x: Any?) {
    if (!!(x !is Nothing?)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>?.<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()
    }
}

// TESTCASE NUMBER: 10
fun case_10(x: Any?) {
    if (!!(x !is Nothing)) else {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!><!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()<!>
    }
}

// TESTCASE NUMBER: 11
fun case_11(x: Any?) {
    if (x is Nothing?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Nothing?")!>x<!>?.<!MISSING_DEPENDENCY_CLASS, MISSING_DEPENDENCY_CLASS!>inv<!>()
    }
}