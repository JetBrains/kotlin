// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// RENDER_DIAGNOSTIC_ARGUMENTS
fun test(a: List<Int?>) {
    val b: List<Int> = a.map { <!RETURN_TYPE_MISMATCH("Int; Int?")!>it?.let { c -> c }<!> }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, nullableType, propertyDeclaration, safeCall */
