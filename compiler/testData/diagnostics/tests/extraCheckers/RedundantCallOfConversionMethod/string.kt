// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
val foo = "".<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toString()<!>

/* GENERATED_FIR_TAGS: propertyDeclaration, stringLiteral */
