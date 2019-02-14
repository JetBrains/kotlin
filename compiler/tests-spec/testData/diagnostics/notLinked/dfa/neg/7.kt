// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 7
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int?) {
    if ((x is Int) <!USELESS_ELVIS!>?: (x is Int)<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>inv()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Int?) {
    if (x?.equals(1) ?: x is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!><!UNSAFE_CALL!>.<!>inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Boolean?) {
    if (x ?: (x != null)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Boolean?) {
    if (if (x != null) <!DEBUG_INFO_SMARTCAST!>x<!> else <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!><!UNSAFE_CALL!>.<!>not()
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    if (if (x is String) true else false) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    if ((if (x != null) true else null) != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
