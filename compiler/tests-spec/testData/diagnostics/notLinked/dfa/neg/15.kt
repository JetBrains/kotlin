// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 15
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

/*
 * TESTCASE NUMBER: 1
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_1() {
    var x: Boolean? = true
    if (x is Boolean && if (true) { x = null; true } else { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 2
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_2() {
    var x: Boolean? = true
    if (x !== null && try { x = null; true } catch (e: Exception) { false }) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 3
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_3() {
    var x: Boolean? = true
    if (x != null) {
        false || when { else -> { x = null; true} }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 4
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_4() {
    var x: Int? = null
    if (x == try { x = 10; null } finally {} && <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int? & kotlin.Nothing")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!>inv()<!>
        <!UNREACHABLE_CODE!>println(1)<!>
    }
}

/*
 * TESTCASE NUMBER: 5
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_5() {
    var x: Boolean? = true
    if (x != null) {
        when { else -> { x = null; false} } || <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean? & kotlin.Nothing"), DEBUG_INFO_SMARTCAST!>x<!>.<!UNREACHABLE_CODE!>not()<!>
    }
}

/*
 * TESTCASE NUMBER: 6
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_6() {
    var x: Boolean? = true
    if (x != null) {
        if (true) {<!UNUSED_VALUE!>x =<!> null; true} else true && <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 7
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_7() {
    var x: Boolean? = true
    if (x != null) {
        (if (true) {x = null; null} else true) ?: <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 8
fun case_8(y: MutableList<Boolean>) {
    var x: Boolean? = true
    if (x != null) {
        y[if (true) {x = null;0} else 0] = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean?"), TYPE_MISMATCH!>x<!>
    }
}

/*
 * TESTCASE NUMBER: 9
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_9() {
    var x: Boolean? = true
    if (x is Boolean) {
        funWithAnyArg(try { x = null; true } catch (e: Exception) { false })
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

/*
 * TESTCASE NUMBER: 10
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-28369
 */
fun case_10() {
    var x: Boolean? = true
    if (x is Boolean) {
        select(if (true) {x = null;Class()} else Class()).prop_9 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Boolean & kotlin.Boolean?"), DEBUG_INFO_SMARTCAST!>x<!>.not()
    }
}

// TESTCASE NUMBER: 11
fun case_11(y: MutableList<MutableList<Int>>) {
    var x: Int? = 10
    if (x != null) {
        y[if (true) {x = null;0} else 0][<!TYPE_MISMATCH!>x<!>] = 10
    }
}
