package test

fun concat(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}

fun <T> noInlineFun(arg: T, f: (T) -> Unit) {
    f(arg)
}

inline fun doSmth(a: String): String {
    return a.toString()
}
