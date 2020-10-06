// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(x: Any?) {
    if (x!! is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?) {
    (x as Nothing?)!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>inv<!>()
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?) {
    if (x as Number? is Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number? & kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?) {
    if (x as Class? is Class) {
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Class? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>prop_1<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?) {
    if (x as Nothing? is Nothing) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>inv<!>()
    }
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?) {
    (x as String?)!!
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>length<!>
}

// TESTCASE NUMBER: 7
fun case_7(x: Any?) {
    if (x as String? != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

// TESTCASE NUMBER: 8
fun case_8(x: Any?) {
    if (x as String? == null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}
