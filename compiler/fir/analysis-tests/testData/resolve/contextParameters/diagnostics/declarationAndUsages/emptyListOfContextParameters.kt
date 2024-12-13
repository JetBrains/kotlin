// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

context(<!SYNTAX!><!>)
fun test1() {}

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>)
fun test2() {}

context(<!SYNTAX!><!>)
val property1: String
    get() = ""

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>String<!>)
val property2: String
    get() = ""

fun inTypePosition(a: context(<!SYNTAX!><!>) ()-> Unit) {}
