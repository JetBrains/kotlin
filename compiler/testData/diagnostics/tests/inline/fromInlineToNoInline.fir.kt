inline fun <R> onlyLocal(p: () -> R) {
    inlineAll(p)
}

inline fun <R> inlineAll(noinline p: () -> R) {
    p()
}