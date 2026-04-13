// RUN_PIPELINE_TILL: BACKEND
fun f(a: List<Number>) = <!USELESS_IS_CHECK!>a is List<Any><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression */
