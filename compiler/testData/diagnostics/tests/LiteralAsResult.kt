// RUN_PIPELINE_TILL: CODEGEN
// Here we want just to check return type
// Should be () -> Int
fun foo() = { 42 }

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral */
