
inline fun <R> onlyLocal(crossinline p: () -> R) {
    inlineAll(p)
}

inline fun <R> inlineAll(p: () -> R) {
    p()
}
