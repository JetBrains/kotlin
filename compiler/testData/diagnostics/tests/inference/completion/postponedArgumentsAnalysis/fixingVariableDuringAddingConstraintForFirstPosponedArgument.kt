// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun f(p: (Int) -> (String) -> Unit) {}

fun g(cond: Boolean) {
    f(if (cond) { i -> { } } else { i -> { } })
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, lambdaLiteral */
