// RUN_PIPELINE_TILL: BACKEND
fun foo() {
    fun convert(vararg paths: String): Array<String> = paths.toList().toTypedArray()

    convert("1", "2", "3")
}

/* GENERATED_FIR_TAGS: functionDeclaration, localFunction, outProjection, stringLiteral, vararg */
