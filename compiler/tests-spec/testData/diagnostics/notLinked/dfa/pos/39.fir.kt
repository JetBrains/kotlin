// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_1(x: Number?) {
    val y: Int? = null

    if (x == y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: Number) {
    val y: Int? = null

    if (x === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Int")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Number) {
    var y: Int? = null

    if (x === y) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Int")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number & kotlin.Int")!>x<!>.inv()
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number?")!>x<!>?.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28265
 */
fun case_5(x: Class, y: Class) {
    if (x.prop_14 == y.prop_15) {
        x.prop_14
        x.prop_14.<!UNRESOLVED_REFERENCE!>inv<!>()
    }
}
