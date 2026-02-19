// RUN_PIPELINE_TILL: BACKEND
class C {
    @Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
    companion object {
        val foo: String?? = ""!! <!USELESS_CAST!>as String??<!>
    }
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, companionObject, nullableType,
objectDeclaration, propertyDeclaration, stringLiteral */
