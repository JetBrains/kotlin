// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

val s: String? = ""
val empty = s?.<!USELESS_CALL_ON_NOT_NULL!>isNullOrEmpty()<!>

/* GENERATED_FIR_TAGS: nullableType, propertyDeclaration, safeCall, stringLiteral */
