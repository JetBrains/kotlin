// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo() {
    @Suppress("warnings")
    ("" as String??)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, nullableType, stringLiteral */
