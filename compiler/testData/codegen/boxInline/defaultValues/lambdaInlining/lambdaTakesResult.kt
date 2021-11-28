// WITH_STDLIB
// SKIP_INLINE_CHECK_IN: inlineFun$default
// IGNORE_BACKEND: JS
// FILE: 1.kt
package test

inline fun <T> inlineFun(v: T, x: (Result<T>) -> T = { it.getOrNull()!! }) =
    x(Result.success(v))

// FILE: 2.kt
import test.*

fun box() = inlineFun("OK")
