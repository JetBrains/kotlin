// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1() {
    var x: Any? = null

    if (true) {
        x = ClassLevel2()
    } else {
        x = ClassLevel3()
    }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassLevel2")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
}
