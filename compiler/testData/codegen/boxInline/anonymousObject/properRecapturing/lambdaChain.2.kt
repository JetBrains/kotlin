package test

inline fun <T> inlineFun(arg: T, f: (T) -> Unit) {
    f(arg)
}
