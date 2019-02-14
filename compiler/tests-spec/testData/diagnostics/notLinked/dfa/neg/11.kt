// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 11
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> is Boolean && if (true) { x = null; true } else { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 2
fun case_2() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> != null && try { x = null; true } catch (e: Exception) { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 3
fun case_3() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> is Boolean) {
        funWithAnyArg(try { x = null; true } catch (e: Exception) { false })
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 4
fun case_4() {
    var x: Boolean? = true
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?")!>x<!> != null) {
        false || when { else -> {x = null; true} }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 5
fun case_5() {
    var x: Int? = null
    if (<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!> == try { x = 10; null } finally {} && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!>inv()<!>
    }
}

// TESTCASE NUMBER: 6
fun case_6() {
    var x: Boolean? = true
    x as Boolean
    if (if (true) { x = null; true } else { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    var x: Boolean? = true
    x!!
    if (if (true) { x = null; true } else { false }) {}

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
}
