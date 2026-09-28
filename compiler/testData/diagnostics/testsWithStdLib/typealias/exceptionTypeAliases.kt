// RUN_PIPELINE_TILL: CODEGEN
// FILE: test.kt
val fooException = Exception("foo")
val barException = kotlin.Exception("bar")

/* GENERATED_FIR_TAGS: propertyDeclaration, stringLiteral */
