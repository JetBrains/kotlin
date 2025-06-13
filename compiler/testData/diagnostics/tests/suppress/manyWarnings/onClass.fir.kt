// RUN_PIPELINE_TILL: BACKEND
@Suppress("REDUNDANT_NULLABLE", "UNNECESSARY_NOT_NULL_ASSERTION")
class C {
    fun foo(): String?? = ""!! as String??
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, functionDeclaration, nullableType,
stringLiteral */
