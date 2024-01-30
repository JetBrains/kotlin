// FILE: 1.kt

inline fun <T> foo(): String {
    return bar<T>()!!.toString()
}

fun <T> bar(): T = "OK" as T

// FILE: 2.kt

fun box() = foo<Int>()