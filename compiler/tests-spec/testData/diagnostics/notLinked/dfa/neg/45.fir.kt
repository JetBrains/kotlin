// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 45
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-22996
 */
fun case_1(x: Number?): Long? {
    if (x is Long?) return x
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number? & kotlin.Number")!>x<!>
    return <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number? & kotlin.Number")!>x<!>.toLong()
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-22997
 */
fun case_2(x: Number?): Long? {
    if (x == null || x is Long) return x else return 0L
}
