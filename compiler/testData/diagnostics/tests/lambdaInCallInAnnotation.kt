// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

annotation class Anno(val x: String)

@Anno(x = listOf({ it }))
fun method() {
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
