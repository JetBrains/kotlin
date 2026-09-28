// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

val s: String? = ""
val blank = s.isNullOrBlank()

/* GENERATED_FIR_TAGS: nullableType, propertyDeclaration, stringLiteral */
