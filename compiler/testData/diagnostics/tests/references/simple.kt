// RUN_PIPELINE_TILL: CODEGEN
fun foo() = 1

fun bar() = foo()

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
