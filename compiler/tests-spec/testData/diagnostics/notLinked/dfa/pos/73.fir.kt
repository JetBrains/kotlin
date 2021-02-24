// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-14257
 */
fun case_1(x: Any?) {
    x is ClassLevel1 || return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel1")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel1")!>x<!>.test1()
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-14257
 */
fun case_2(x: Any?) {
    x !is ClassLevel1 && return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel1")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel1")!>x<!>.test1()
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-14257
 */
fun case_3(x: Any?) {
    x as? ClassLevel1 ?: return
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel1")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel1")!>x<!>.test1()
}
