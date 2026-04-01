// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-37060

// KT-37060: not-null assertion breaks inference of lambda parameter types
fun foo(fn: (x: Int, y: Int) -> Int) {}

fun test() {
    foo({ x, y -> x + y }<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>)
}

/* GENERATED_FIR_TAGS: additiveExpression, checkNotNullCall, functionDeclaration, functionalType, lambdaLiteral */
