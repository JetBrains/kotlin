// RUN_PIPELINE_TILL: BACKEND
fun f(x: Any) = x <!UNCHECKED_CAST!>as Array<String><!>

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration */
