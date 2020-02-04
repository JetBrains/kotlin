// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 46
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int) = ""
fun case_1(x: Int?) = 10
fun case_1() {
    var x: Int? = 10
    var y = { x = null }
    if (x != null) {
        val z = case_1(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}

// TESTCASE NUMBER: 2
val case_2_prop: Int?
    get() = 10
fun case_2(x: Int) = ""
fun case_2(x: Int?) = 10
fun case_2() {
    if (case_2_prop != null) {
        val z = case_2(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>case_2_prop<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}

// TESTCASE NUMBER: 3
class Case4 {
    var x: Int? = 10
}
fun case_3(x: Int) = ""
fun case_3(x: Int?) = 10
fun case_3(y: Case4) {
    if (y.x != null) {
        val z = case_3(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>y.x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}

// TESTCASE NUMBER: 4
open class Case5 {
    open val x: Int? = 10
}
fun case_4(x: Int) = ""
fun case_4(x: Int?) = 10
fun case_4(y: Case4) {
    if (y.x != null) {
        val z = case_4(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>y.x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}

// TESTCASE NUMBER: 5
class Case6 {
    val x: Int? by lazy { 10 }
}
fun case_5(x: Int) = ""
fun case_5(x: Int?) = 10
fun case_5(y: Case4) {
    if (y.x != null) {
        val z = case_5(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>y.x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}

// TESTCASE NUMBER: 6
var case_6_prop: Int?
    get() = 10
    set(value) {}
fun case_6(x: Int) = ""
fun case_6(x: Int?) = 10
fun case_6() {
    if (case_6_prop != null) {
        val z = case_6(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?"), SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>case_6_prop<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}
