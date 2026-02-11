// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT

context(String)
fun foo(x: Comparable<*>) {}

context(String)
fun foo(x: Number) {}

fun test() {
    with("") {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1)
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, lambdaLiteral,
starProjection, stringLiteral */
