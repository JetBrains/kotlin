package test

inline fun <R> call(s: () -> R) = s()

inline fun test(crossinline z: () -> String) = { z() }
