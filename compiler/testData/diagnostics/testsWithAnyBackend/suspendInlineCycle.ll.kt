// LL_FIR_DIVERGENCE
// FIR checkers don't raise backend's INLINE_CALL_CYCLE diagnostic.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

suspend inline fun inlineFun1(p: () -> Unit) {
    p()
    inlineFun2(p)
}

suspend inline fun inlineFun2(p: () -> Unit) {
    p()
    inlineFun1(p)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, inline, suspend */
