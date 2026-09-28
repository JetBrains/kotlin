// RUN_PIPELINE_TILL: CODEGEN
@Suppress("REDUNDANT_NULLABLE")
class C {
    fun foo(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, nullableType, stringLiteral */
