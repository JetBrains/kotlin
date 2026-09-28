// RUN_PIPELINE_TILL: CODEGEN
// FILE: a.kt
val x = 1

// FILE: b.kt
fun getX() = 1

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, propertyDeclaration */
