// RUN_PIPELINE_TILL: FRONTEND

fun f(a : MutableList<out Any>) = a is <!CANNOT_CHECK_FOR_ERASED!>MutableList<out Int><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, outProjection */
