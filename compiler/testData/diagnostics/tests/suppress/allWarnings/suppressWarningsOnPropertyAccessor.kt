// RUN_PIPELINE_TILL: BACKEND
class C {
    val foo: String?
        @Suppress("warnings")
        get(): String?? = null as Nothing?
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, getter, nullableType, propertyDeclaration, stringLiteral */
