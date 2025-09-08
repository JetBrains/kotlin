// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// Here we want just to check return type
// Should be () -> Int
fun foo() = { 42 }

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral */
