package test

inline fun <R> doCall(p: () -> R) {
    p()
}