// RUN_PIPELINE_TILL: CODEGEN

fun ff(a: Any) = a <!UNCHECKED_CAST!>as MutableList<String><!>

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration */
