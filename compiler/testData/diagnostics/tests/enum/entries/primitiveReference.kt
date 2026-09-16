// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

enum class Some {}

val x = Some::entries

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, propertyDeclaration */
