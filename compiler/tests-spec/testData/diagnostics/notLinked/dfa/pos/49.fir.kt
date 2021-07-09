// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29935
 */
fun case_1(x: Any?) {
    if (x is Int == @RetentionSourceAndTargetExpression true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & kotlin.Int")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29935
 */
fun case_2(x: Int?) {
    if (x != @RetentionSourceAndTargetExpression null == true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29935
 */
fun case_3(x: Int?, y: Int) {
    if (x == y == @RetentionSourceAndTargetExpression true) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-29935
 */
fun case_4(x: Int?, y: Int) {
    if (@RetentionSourceAndTargetExpression !!(x == y)) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Int")!>x<!>.inv()
    }
}
