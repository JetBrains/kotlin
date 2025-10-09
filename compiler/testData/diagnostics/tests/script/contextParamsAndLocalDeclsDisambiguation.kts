// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

class C

context(x: Any)
fun contextFun() {}

context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>C<!><!SYNTAX!><!SYNTAX!><!>::<!>class<!SYNTAX!>)<!> {
    <!SYNTAX!>contextFun<!><!SYNTAX!>(<!><!SYNTAX!>)<!>
}

context(<!SYNTAX!><!>fun() {}<!SYNTAX!>)<!> {
    contextFun()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, propertyDeclaration,
starProjection */
