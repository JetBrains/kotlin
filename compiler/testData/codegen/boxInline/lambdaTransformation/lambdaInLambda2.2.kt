package test

inline fun <R> mfun(f: () -> R) {
    f()
}

fun noInline(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}