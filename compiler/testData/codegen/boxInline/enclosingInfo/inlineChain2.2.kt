package test

inline fun <R> call(crossinline s: () -> R) = { s() }()

inline fun test(crossinline z: () -> String) = { z() }
