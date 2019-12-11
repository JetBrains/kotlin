
inline fun <R> toOnlyLocal(crossinline p: () -> R) {
    p()
}

inline fun <R> inlineAll(p: () -> R) {
    toOnlyLocal(p)
}
