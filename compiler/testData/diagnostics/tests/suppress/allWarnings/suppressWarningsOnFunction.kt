// RUN_PIPELINE_TILL: CODEGEN
class C {
    @Suppress("warnings")
    fun foo(p: String??) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, stringLiteral */
