package test

inline fun <T, R> mfun(arg: T, f: (T) -> R) : R {
    return f(arg)
}

inline fun <T> doSmth(a: T): String {
    return a.toString()
}