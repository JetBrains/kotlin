// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_PARAMETER
fun f(p: Int): Int {
    val p = 2
    return p
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
