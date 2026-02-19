// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(l: () -> Unit) {}
fun bar(l: () -> String) {}

val a = foo { <!UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!> }
val b = bar { <!RETURN_TYPE_MISMATCH, UNSUPPORTED_ARRAY_LITERAL_OUTSIDE_OF_ANNOTATION_ERROR!>[]<!> }

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, functionalType, lambdaLiteral, propertyDeclaration */
