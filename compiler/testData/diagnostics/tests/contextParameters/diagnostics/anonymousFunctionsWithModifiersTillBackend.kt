// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters
val t2 = context(a: String) tailrec fun() {}

/* GENERATED_FIR_TAGS: anonymousFunction, propertyDeclaration */
