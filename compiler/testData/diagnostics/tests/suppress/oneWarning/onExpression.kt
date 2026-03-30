// RUN_PIPELINE_TILL: BACKEND
fun foo(): Any? {
    @Suppress("REDUNDANT_NULLABLE")
    return null <!USELESS_CAST!>as Nothing??<!>
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, nullableType, stringLiteral */
