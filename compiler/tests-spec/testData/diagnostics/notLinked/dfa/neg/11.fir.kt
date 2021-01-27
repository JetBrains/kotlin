// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> is Boolean && if (true) { x = null; true } else { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.not()
    }
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> != null && try { x = null; true } catch (e: Exception) { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>.not()
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> is Boolean) {
        funWithAnyArg(try { x = null; true } catch (e: Exception) { false })
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!UNSAFE_CALL!>not<!>()
    }
}

// TESTCASE NUMBER: 4
fun case_4() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> != null) {
        false || when { else -> {x = null; true} }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!UNSAFE_CALL!>not<!>()
    }
}

/*
 * TESTCASE NUMBER: 5
 * ISSUES: KT-35668
 */
fun case_5() {
    var x: Int? = null
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!> == try { x = 10; null } finally {} && x != null) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>.inv()
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    var x: Boolean? = true
    x as Boolean
    if (if (true) { x = null; true } else { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!UNSAFE_CALL!>not<!>()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Boolean? = true
    x!!
    if (if (true) { x = null; true } else { false }) {}

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Boolean?")!>x<!>.<!UNSAFE_CALL!>not<!>()
}
