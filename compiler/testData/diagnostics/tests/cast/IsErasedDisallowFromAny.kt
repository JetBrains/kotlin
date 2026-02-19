// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

fun ff(l: Any) = l is <!CANNOT_CHECK_FOR_ERASED!>MutableList<String><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression */
