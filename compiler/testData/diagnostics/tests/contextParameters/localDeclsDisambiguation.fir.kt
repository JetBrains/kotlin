// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class C

context(x: Any)
fun contextFun() {}

fun test() {
    context(<!CONTEXT_PARAMETER_WITHOUT_NAME!>C<!><!SYNTAX!><!SYNTAX!><!>::<!>class<!SYNTAX!>)<!> {
        <!SYNTAX!>contextFun<!><!SYNTAX!>(<!><!SYNTAX!>)<!>
    }

    context(<!SYNTAX!><!>fun () {} <!SYNTAX!>)<!> {
        <!NO_CONTEXT_ARGUMENT!>contextFun<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext */
