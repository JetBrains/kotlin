// RUN_PIPELINE_TILL: BACKEND
class C {
    val foo: String?
        @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
        get(): String?? = ""!! <!USELESS_CAST!>as String??<!>
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, getter, nullableType, propertyDeclaration,
stringLiteral */
