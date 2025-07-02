// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

@Deprecated("")
context(_: String)
fun foo() {}

context(_: String)
fun bar() {
    <!DEPRECATION("context(_: String) fun foo(): Unit")!>foo<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, stringLiteral */
