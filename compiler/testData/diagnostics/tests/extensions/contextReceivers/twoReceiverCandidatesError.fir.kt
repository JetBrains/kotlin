// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

fun String.foo() {}

context(Int, Double)
fun bar() {
    <!UNRESOLVED_REFERENCE!>foo<!>() // should be prohibited
}

fun main() {
    with(1) {
        with(2.0) {
            bar()
        }
    }
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, integerLiteral,
lambdaLiteral */
