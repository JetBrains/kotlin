// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class Case1<T : Number> {
    inline fun <reified T : CharSequence>case_1(x: Any?) {
        if (x is T) {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & T")!>x<!>.<!INAPPLICABLE_CANDIDATE!>toByte<!>()
        }
    }
}
