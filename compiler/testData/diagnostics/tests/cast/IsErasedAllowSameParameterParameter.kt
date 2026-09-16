// RUN_PIPELINE_TILL: CODEGEN

fun <T> ff(l: MutableCollection<T>) = l is MutableList<T>

/* GENERATED_FIR_TAGS: functionDeclaration, isExpression, nullableType, typeParameter */
