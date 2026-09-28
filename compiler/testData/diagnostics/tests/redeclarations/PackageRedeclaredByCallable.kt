// RUN_PIPELINE_TILL: CODEGEN
// FILE: a.kt
package a

// FILE: b.kt
fun a() {}

val a = ""

/* GENERATED_FIR_TAGS: functionDeclaration, propertyDeclaration, stringLiteral */
