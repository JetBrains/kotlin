// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

annotation class Anno(val x: String)

@Anno(x = listOf({ it }))
fun method() {
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
