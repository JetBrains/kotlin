// RUN_PIPELINE_TILL: BACKEND
class C {
    fun foo(@Suppress("REDUNDANT_NULLABLE") p: String?? = null <!USELESS_CAST!>as Nothing??<!>) = p
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, nullableType, stringLiteral */
