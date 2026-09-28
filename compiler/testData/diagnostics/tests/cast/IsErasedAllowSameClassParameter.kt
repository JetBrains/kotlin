// RUN_PIPELINE_TILL: CODEGEN

fun ff(l: MutableCollection<String>) = l is MutableList<String>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression */
