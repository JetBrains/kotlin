package test

inline fun <R> mfun(f: () -> R) {
    f()
}

fun concat(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}