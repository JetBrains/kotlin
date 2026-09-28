// RUN_PIPELINE_TILL: CODEGEN
fun foo() {
    @Suppress("warnings")
    ("" as String??)
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, nullableType, stringLiteral */
