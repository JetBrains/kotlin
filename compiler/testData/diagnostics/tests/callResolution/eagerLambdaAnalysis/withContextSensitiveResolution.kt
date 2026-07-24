// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EagerLambdaAnalysis, +ContextSensitiveResolutionUsingExpectedType
// ISSUES: KT-86533

enum class Problem {
    CONNECTION
}

fun foo(problem: () -> Problem) = "(1)"

fun foo(problem: () -> Unit) = "(2)"

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { <!UNRESOLVED_REFERENCE!>CONNECTION<!> }
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
