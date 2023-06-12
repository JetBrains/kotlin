// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 40
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-8819
 */
fun case_1(x: Pair<*, *>) {
    if (x.first !is String) return
    x.first
    <!SMARTCAST_IMPOSSIBLE!>x.first<!>.length
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-8819
 */
fun case_2(x: Pair<*, *>) {
    if (x.first !is String?) throw Exception()
    x.first
    x.first?.<!UNRESOLVED_REFERENCE!>length<!>
}
