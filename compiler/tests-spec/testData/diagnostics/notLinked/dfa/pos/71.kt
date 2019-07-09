// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 71
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-19446
 */
fun case_1() {
    run {
        var unit: Unit? = Unit
        while (unit != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit & kotlin.Unit?")!>unit<!>
            <!UNRESOLVED_REFERENCE!>consume<!>(unit)
            unit = null
        }
    }

    run {
        var unit: Unit? = Unit
        while (unit != null) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Unit?")!>unit<!>
            <!UNRESOLVED_REFERENCE!>consume<!>(unit)
            unit = null
        }
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-10420
 */
fun case_2(): Int {
    fun b(): Int {
        var c: Int? = null
        if (c == null || 0 < <!SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>c<!>) c = 0
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>c<!>
        return c ?: 0
    }

    var c: Int = 0
    c = 0
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>c<!>
    return c
}