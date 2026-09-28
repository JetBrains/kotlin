// RUN_PIPELINE_TILL: CODEGEN
fun bar(b: Boolean) = b

fun foo(data: List<String>) {
    bar(data.contains(""))
}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */
