// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(a: Int)
fun foo0() {}

context(a: Int, b: String)
fun foo1() {}

context(a: Int)
fun foo2(b: String) {}

fun test() {
    foo0(a = 42, <!ARGUMENT_PASSED_TWICE!>a<!> = 4)
    foo0(a = 42, <!ARGUMENT_PASSED_TWICE!>a<!> = 42)
    foo1(a = 42, b = "42", <!ARGUMENT_PASSED_TWICE!>a<!> = 42)
    foo2(a = 42, b = "42", <!ARGUMENT_PASSED_TWICE!>a<!> = 42)
    foo2("42", a = 42, <!ARGUMENT_PASSED_TWICE!>a<!> = 42)
    foo2("42", a = 42, <!ARGUMENT_PASSED_TWICE!>b<!> = "42", <!ARGUMENT_PASSED_TWICE!>a<!> = 42)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
operator, stringLiteral */
