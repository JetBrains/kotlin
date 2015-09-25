
inline fun <R> inlineFunWithAnnotation(crossinline p: () -> R) {
    inlineFun {
        p()
    }
}

inline fun <R> inlineFun2(p: () -> R) {
    inlineFun {
        p()
    }
}

inline fun <R> inlineFun(p: () -> R) {
    p()
}
