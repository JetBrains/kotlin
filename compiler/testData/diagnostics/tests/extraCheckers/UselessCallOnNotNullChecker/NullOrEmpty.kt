// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB

val s = ""
val empty = s.<!USELESS_CALL_ON_NOT_NULL!>isNullOrEmpty()<!>

/* GENERATED_FIR_TAGS: propertyDeclaration, stringLiteral */
