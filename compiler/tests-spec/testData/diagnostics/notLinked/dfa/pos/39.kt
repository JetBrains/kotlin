// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 39
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_1(x: Number?) {
    val y: Int? = null

    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Number) {
    val y: Int? = null

    if (x === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Number) {
    var y: Int? = null

    if (x === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Number"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_4() {
    var x: Number? = null
    var y: Int? = null

    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>?.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_5(x: Class, y: Class) {
    if (x.prop_14 == y.prop_15) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>?")!>x.prop_14<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>?")!>x.prop_14<!>.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
    }
}
