// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// TARGET_BACKEND: JVM
// FILE: 1.kt
package test

inline fun <R> mfun(f: () -> R) {
    f()
}

fun noInline(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}

// FILE: 2.kt

import test.*
import java.util.*

fun test1(prefix: String): String {
    var result = "fail"
    mfun {
        noInline("start") {
            if (it.startsWith(prefix)) {
                result = "OK"
            }
        }
    }

    return result
}

fun box(): String {
    if (test1("start") != "OK") return "fail1"
    if (test1("nostart") != "fail") return "fail2"

    return "OK"
}
