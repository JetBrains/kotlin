// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 53
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int) = ""
fun case_1(x: Int?) = 10
fun case_1() {
    var x: Int? = 10
    if (x != null) {
        val z = case_1(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>z<!>
    }
}
