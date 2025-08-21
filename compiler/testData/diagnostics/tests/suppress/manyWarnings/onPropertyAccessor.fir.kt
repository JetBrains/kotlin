// RUN_PIPELINE_TILL: BACKEND
class C {
    val foo: String?
        @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
        get(): String?? = ""!! as String??
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, getter, nullableType, propertyDeclaration,
stringLiteral */
