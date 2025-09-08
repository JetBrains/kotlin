// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
@Suppress("warnings")
val anonymous = object {
    fun foo(p: String?? = "" as String) {}
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, asExpression, functionDeclaration, nullableType, propertyDeclaration,
stringLiteral */
