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

    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>x<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ClassLevel2 & kotlin.Any?")!>x<!>.<!UNRESOLVED_REFERENCE!>inv<!>()
}
