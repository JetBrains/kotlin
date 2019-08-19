// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 72
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-19446
 */
fun case_1() {
    val list = mutableListOf<String>()
    val ints = list <!UNCHECKED_CAST!>as MutableList<Int><!>
    val strs = list as MutableList<String>
    strs.add("two")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.String>")!>list<!>
    val s: String = <!TYPE_MISMATCH, TYPE_MISMATCH!><!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.MutableList<kotlin.Int> & kotlin.collections.MutableList<kotlin.String>"), DEBUG_INFO_SMARTCAST!>list<!>[0]<!>
}