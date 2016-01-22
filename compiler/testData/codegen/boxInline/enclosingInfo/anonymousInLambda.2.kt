package test

inline fun <R> call(s: () -> R) = s()
