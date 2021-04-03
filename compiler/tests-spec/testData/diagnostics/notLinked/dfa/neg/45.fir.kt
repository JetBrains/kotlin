// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-22996
 */
fun case_1(x: Number?): Long? {
    if (x is Long?) return x
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>
    return <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!><!UNSAFE_CALL!>.<!>toLong()
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-22997
 */
fun case_2(x: Number?): Long? {
    if (x == null || x is Long) return <!RETURN_TYPE_MISMATCH!>x<!> else return 0L
}
