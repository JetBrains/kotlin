// RUN_PIPELINE_TILL: BACKEND
fun f(a: MutableList<out Number>) = <!USELESS_IS_CHECK!>a is MutableList<out Any><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, outProjection */
