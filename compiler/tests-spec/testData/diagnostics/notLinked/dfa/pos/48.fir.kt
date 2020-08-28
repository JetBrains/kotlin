// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_1(x: Any?, y: Any?) {
    if (x!! === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>y<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_2(x: Any?, y: Any?) {
    if (x as Int === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>inv<!>(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>inv<!>(10)
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_3(x: Any?, y: Any?) {
    if (y === x!!) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>x<!>.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & kotlin.Any?")!>y<!>.equals(10)
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-30317
 */
fun case_4(x: Any?, y: Any?) {
    if (y === x as Int) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>x<!>.<!INAPPLICABLE_CANDIDATE!>inv<!>(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Any?")!>y<!>.<!INAPPLICABLE_CANDIDATE!>inv<!>(10)
    }
}
