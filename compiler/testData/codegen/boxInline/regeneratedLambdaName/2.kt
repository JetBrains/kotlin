package test

inline fun <R> call(f: () -> R) : R {
    return {f()} ()
}
