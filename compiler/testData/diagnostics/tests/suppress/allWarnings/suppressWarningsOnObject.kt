// RUN_PIPELINE_TILL: CODEGEN
@Suppress("warnings")
object C {
    fun foo(p: String??) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, objectDeclaration, stringLiteral */
