// LL_FIR_DIVERGENCE
// FIR checkers don't raise backend's INLINE_CALL_CYCLE diagnostic.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline fun inlineFun1(crossinline p: () -> Unit) {
    object {
        fun method() { inlineFun2(p) }
    }
}

inline fun inlineFun2(crossinline p: () -> Unit) {
    inlineFun1(p)
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, crossinline, functionDeclaration, functionalType, inline */
