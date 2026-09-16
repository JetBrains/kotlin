// RUN_PIPELINE_TILL: CODEGEN
fun f(x: Any) = x <!UNCHECKED_CAST!>as Array<String><!>

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration */
