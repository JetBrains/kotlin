// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE

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
