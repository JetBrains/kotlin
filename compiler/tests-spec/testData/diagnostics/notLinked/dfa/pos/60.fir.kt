// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    var x: String? = null

    outer@ while (x != null) {
        inner@ do {
            x = null
        } while (x == null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String? & kotlin.String")!>x<!>.length
    }
}
