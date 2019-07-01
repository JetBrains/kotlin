// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 42
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-19446
 */
fun case_1() {
    var x: Boolean? = true
    x!!
    val y = {
        val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, NAME_SHADOWING!>x<!>: Int?
        x = 10
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?"), SMARTCAST_IMPOSSIBLE!>x<!>.equals(10)
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-19446
 */
fun case_2() {
    var x: Boolean? = true
    x!!
    val y = {
        var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE, NAME_SHADOWING!>x<!>: Int? = 10
        x = 10
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?"), SMARTCAST_IMPOSSIBLE!>x<!>.equals(10)
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Boolean? = true
    x!!
    val y = {
        var <!NAME_SHADOWING!>x<!>: Int? = 10
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.equals(10)
}