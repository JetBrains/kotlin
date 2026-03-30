// RUN_PIPELINE_TILL: BACKEND
fun f(a: MutableList<String>) = <!USELESS_IS_CHECK!>a is MutableList<out CharSequence><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, outProjection */
