// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class A

context(a: A)
fun foo(vararg s: String) {}

fun test() {
    foo(a = A())
    foo("", a = A())
    foo("", "", a = A())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, stringLiteral, vararg */
