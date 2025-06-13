// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

inline fun <R> inlineFunOnlyLocal(crossinline p: () -> R) {
    {
        p()
    }()
}

inline fun <R> inlineFun(p: () -> R) {
    {
        <!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>()
    }()
}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionalType, inline, lambdaLiteral, nullableType,
typeParameter */
