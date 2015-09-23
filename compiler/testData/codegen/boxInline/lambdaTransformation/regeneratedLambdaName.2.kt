package test


inline fun <R> call(crossinline f: () -> R) : R {
    return {f()} ()
}
