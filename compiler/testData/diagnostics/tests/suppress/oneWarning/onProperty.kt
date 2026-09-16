// RUN_PIPELINE_TILL: CODEGEN
class C {
    @Suppress("REDUNDANT_NULLABLE")
    val foo: String?? = null <!USELESS_CAST!>as Nothing?<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, nullableType, propertyDeclaration, stringLiteral */
