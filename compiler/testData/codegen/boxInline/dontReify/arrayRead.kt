// FILE: 1.kt

inline fun <T> f(arr: Array<T>, func: (T) -> Int): Int = func(arr[0])

// FILE: 2.kt

fun box(): String = ('O' + f(arrayOf(""), String::length)).toString() + "K"