// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class C {
    @Suppress("warnings")
    val foo: String?? = null as Nothing?
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, nullableType, propertyDeclaration, stringLiteral */
