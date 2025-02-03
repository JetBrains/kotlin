// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A

context(<!CONTEXT_PARAMETER_WITH_DEFAULT!>a: A = A()<!>)
fun test() {}

context(<!CONTEXT_PARAMETER_WITH_DEFAULT!>a: A = A()<!>)
val testProperty
    get() = 1
