// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    @Suppress("REDUNDANT_NULLABLE")
    fun foo(): String?? = null <!USELESS_CAST!>as Nothing??<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, nullableType, stringLiteral */
