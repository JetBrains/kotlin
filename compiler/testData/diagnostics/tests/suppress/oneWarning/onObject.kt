// RUN_PIPELINE_TILL: CODEGEN
@Suppress("REDUNDANT_NULLABLE")
object C {
    fun foo(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, nullableType, objectDeclaration, stringLiteral */
