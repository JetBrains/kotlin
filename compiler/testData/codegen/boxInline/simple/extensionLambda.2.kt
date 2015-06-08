package test

inline fun <T> String.test(default: T, cb: String.(T) -> T): T = cb(default)