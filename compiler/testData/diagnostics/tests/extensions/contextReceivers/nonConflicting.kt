// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers, -ContextParameters

interface A
interface B
interface C
interface D

context(A, B)
fun f(): Unit = TODO()

context(C, D)
fun f(): Unit = TODO()

fun test(a: A, b: B) {
    with(a) {
        with(b) {
            f()
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, lambdaLiteral */
