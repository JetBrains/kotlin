// FILE: 1.kt

package test

inline fun <R> mfun(f: () -> R) {
    f()
    f()
}

public inline fun String.toLowerCase2() : String = ""

// FILE: 2.kt

import test.*

fun box(): String {
    mfun{ "".toLowerCase2() }
    return "OK"
}
