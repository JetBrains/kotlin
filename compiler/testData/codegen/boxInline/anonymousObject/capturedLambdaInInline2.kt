// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

inline fun bar(crossinline y: () -> String) = {
    { { call(y) }.let { it() } }.let { it() }
}

public inline fun <T> call(f: () -> T): T = f()

// FILE: 2.kt

import test.*

fun box(): String {
    return bar {"OK"} ()
}
