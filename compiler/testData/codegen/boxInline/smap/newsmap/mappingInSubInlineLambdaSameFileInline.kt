// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun test(s: () -> Unit) {
    val z = 1;
    s()
    val x = 1;
}

// FILE: 2.kt

import test.*

inline fun test2(s: () -> String): String {
    val z = 1;
    val res = s()
    return res
}

fun <T> eval(f: () -> T) = f()

fun box(): String {
    var result = "fail"

    test {
        eval {
            result = test2 {
                "OK"
            }
        }
    }

    return result
}
