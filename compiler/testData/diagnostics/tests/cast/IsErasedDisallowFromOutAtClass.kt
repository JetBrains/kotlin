// RUN_PIPELINE_TILL: FRONTEND
fun f(a: List<Any>) = a is <!CANNOT_CHECK_FOR_ERASED!>List<Number><!>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression */
