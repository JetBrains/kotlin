// FILE: 1.kt

package test

inline fun <T, R> mfun(arg: T, f: (T) -> R) : R {
    return f(arg)
}

inline fun <T> doSmth(a: T): String {
    return a.toString()
}

// FILE: 2.kt

import test.*

fun test1(s: Long): String {
    var result = "OK"
    result = mfun(s) { a ->
        result + doSmth(s) + doSmth(a)
    }

    return result
}

fun box(): String {
    val result = test1(11.toLong())
    if (result != "OK1111") return "fail1: ${result}"

    return "OK"
}
