// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

val list: List<String>? = null
val empty = list.orEmpty()

/* GENERATED_FIR_TAGS: nullableType, propertyDeclaration */
