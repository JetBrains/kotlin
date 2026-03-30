// RUN_PIPELINE_TILL: BACKEND

fun ff(c: MutableCollection<String>) = c <!UNCHECKED_CAST!>as MutableList<Int><!>

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration */
