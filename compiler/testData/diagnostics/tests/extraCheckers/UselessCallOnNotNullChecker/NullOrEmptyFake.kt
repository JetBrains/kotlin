// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

val s: String? = null
val empty = s.isNullOrEmpty()

/* GENERATED_FIR_TAGS: nullableType, propertyDeclaration */
