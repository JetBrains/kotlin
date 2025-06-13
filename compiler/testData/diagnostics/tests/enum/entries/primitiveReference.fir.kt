// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +EnumEntries
// WITH_STDLIB

enum class Some {}

val x = Some::entries

/* GENERATED_FIR_TAGS: callableReference, enumDeclaration, propertyDeclaration */
