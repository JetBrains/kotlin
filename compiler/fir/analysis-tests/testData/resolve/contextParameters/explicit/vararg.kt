// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

class A
class B

context(a: A)
fun foo(vararg s: String) {}

fun test() {
    foo(a = A())
    foo("", a = A())
    foo("", "", a = A())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, stringLiteral, vararg */
