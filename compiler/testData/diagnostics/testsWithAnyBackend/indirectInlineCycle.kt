// RUN_PIPELINE_TILL: BACKEND
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

inline fun inlineFun1(crossinline p: () -> Unit) {
    object {
        fun method() { <!INLINE_CALL_CYCLE!>inlineFun2(p)<!> }
    }
}

inline fun inlineFun2(crossinline p: () -> Unit) {
    <!INLINE_CALL_CYCLE!>inlineFun1(p)<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, crossinline, functionDeclaration, functionalType, inline */
