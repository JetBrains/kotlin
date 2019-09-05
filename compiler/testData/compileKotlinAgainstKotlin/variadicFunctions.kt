// TARGET_BACKEND: JVM
// !LANGUAGE: +NewInference
// FILE: A.kt

package a

fun <vararg Ts, R> variadic(
    vararg arguments: *Ts,
    transform: (*Ts) -> R
) = transform(arguments)

// FILE: B.kt

import a.variadic

fun <T1, T2, R> adapter(
    arg1: T1,
    arg2: T2,
    transform: (T1, T2) -> R
) = variadic(arg1, arg2) { first, second -> transform(first, second) }

fun box(): String = adapter("O", "K") { o, k -> o + k }
