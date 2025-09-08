// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Suppress("REDUNDANT_NULLABLE")
object C {
    fun foo(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, nullableType, objectDeclaration, stringLiteral */
