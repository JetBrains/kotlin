// FILE: 1.kt

package test

inline fun <T, R> T.map(transform: (T) -> R): R {
    return transform(this)
}

// FILE: 2.kt

import test.*

fun box(): String {
    val result = 1.map(2::plus)
    return if (result == 3) "OK" else "fail $result"
}