// RUN_PIPELINE_TILL: BACKEND

fun foo(bar: Any?): Int {
    bar as String?
    bar ?: throw IllegalStateException()
    return bar.length
}

/* GENERATED_FIR_TAGS: asExpression, elvisExpression, functionDeclaration, nullableType, smartcast */
