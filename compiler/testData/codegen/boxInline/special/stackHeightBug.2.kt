package test

inline fun <R> mfun(f: () -> R) {
    f()
    f()
}

public inline fun String.toLowerCase2() : String = ""