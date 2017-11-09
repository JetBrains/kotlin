// FILE: 1.kt

package test

inline fun <T, R> T.map(transform: (T) -> R): R {
    return transform(this)
}

// FILE: 2.kt

import test.*

fun box(): String {
    val result = 1.map(3L::plus)
    return if (result == 4L) "OK" else "fail $result"
}