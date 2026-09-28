// RUN_PIPELINE_TILL: CODEGEN
interface IC {
    fun toString(x: String): String = "IC$x"
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration, stringLiteral */
