// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +InlineClasses

fun result(): Result<Int> = TODO()
val resultP: Result<Int> = result()

/* GENERATED_FIR_TAGS: functionDeclaration, propertyDeclaration */
