// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_VALUE -VARIABLE_WITH_REDUNDANT_INITIALIZER
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(x: Interface1) = x
fun case_1(x: Interface2) = x
fun case_1() {
    val x: Interface1 = null as Interface1
    x as Interface2
    <!AMBIGUITY!>case_1<!>(<!DEBUG_INFO_EXPRESSION_TYPE("Interface2 & Interface1 & Interface1")!>x<!>)
}
