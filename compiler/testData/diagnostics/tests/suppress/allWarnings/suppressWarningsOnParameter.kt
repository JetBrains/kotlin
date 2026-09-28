// RUN_PIPELINE_TILL: CODEGEN
class C {
    fun foo(@Suppress("warnings") p: String?? = "" as String) {}
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, nullableType, stringLiteral */
