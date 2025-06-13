// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun f(a: MutableList<String>) = <!USELESS_IS_CHECK!>a is MutableList<out CharSequence><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, outProjection */
