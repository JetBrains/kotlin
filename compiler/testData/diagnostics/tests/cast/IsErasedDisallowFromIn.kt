// RUN_PIPELINE_TILL: FRONTEND
fun f(a: MutableList<in String>) = a is <!CANNOT_CHECK_FOR_ERASED!>MutableList<CharSequence><!>

/* GENERATED_FIR_TAGS: functionDeclaration, inProjection, isExpression */
