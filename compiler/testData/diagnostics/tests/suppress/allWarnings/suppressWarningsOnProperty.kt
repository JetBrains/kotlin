// RUN_PIPELINE_TILL: CODEGEN
class C {
    @Suppress("warnings")
    val foo: String?? = null as Nothing?
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, nullableType, propertyDeclaration, stringLiteral */
