// FIR_IDENTICAL

inline fun <R> toOnlyLocal(crossinline p: () -> R) {
    p()
}

inline fun <R> inlineAll(p: () -> R) {
    toOnlyLocal(<!NON_LOCAL_RETURN_NOT_ALLOWED!>p<!>)
}
