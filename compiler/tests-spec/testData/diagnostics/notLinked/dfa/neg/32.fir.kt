// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

// TESTCASE NUMBER: 1, 2, 3, 4, 5
fun stringArg(number: String) {}

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464, KT-35668
 */
fun case_1(x: Int?) {
    if (x == null) {
        <!INAPPLICABLE_CANDIDATE!>stringArg<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x!!<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464, KT-35668
 */
fun case_2(x: Int?, y: Nothing?) {
    if (x == y) {
        <!INAPPLICABLE_CANDIDATE!>stringArg<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x!!<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_3(x: Int?) {
    if (x == null) {
        x as Int
        <!INAPPLICABLE_CANDIDATE!>stringArg<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_4(x: Int?) {
    if (x == null) {
        x!!
        <!INAPPLICABLE_CANDIDATE!>stringArg<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-27464
 */
fun case_5(x: Int?) {
    if (x == null) {
        var y = x
        y!!
        <!INAPPLICABLE_CANDIDATE!>stringArg<!>(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>y<!>
    }
}
