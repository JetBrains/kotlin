// FILE: 1.kt

package test

inline fun <T> String.test(default: T, cb: String.(T) -> T): T = cb(default)

// FILE: 2.kt

import test.*

fun box(): String {
    val p = "".test(50.0) {
        it
    }

    return if (p == 50.0) "OK" else "fail $p"
}
