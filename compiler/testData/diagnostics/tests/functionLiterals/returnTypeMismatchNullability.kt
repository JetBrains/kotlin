// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_MESSAGES
fun test(a: List<Int?>) {
    val b: List<Int> = a.map { <!TYPE_MISMATCH("Int; Int?")!>it?.let { c -> c }<!> }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
