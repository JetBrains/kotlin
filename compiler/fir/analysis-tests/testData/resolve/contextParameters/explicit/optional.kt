// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// LANGUAGE: +ContextParameters

class A

context(a: A) fun foo() { }
fun foo(a: A = A(), x: Int = 1) { }

fun test() {
    foo()
    foo(a = A())
    foo(a = A(), x = 2)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral */
