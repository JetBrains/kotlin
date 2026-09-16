// RUN_PIPELINE_TILL: CODEGEN
fun test(
    f: String.() -> Int = { length }
): Int {
    return "".f()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, stringLiteral, typeWithExtension */
