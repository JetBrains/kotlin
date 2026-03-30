// RUN_PIPELINE_TILL: FRONTEND

fun f(c: LongRange): Int {
    return c.<!FUNCTION_EXPECTED!>start<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration */
