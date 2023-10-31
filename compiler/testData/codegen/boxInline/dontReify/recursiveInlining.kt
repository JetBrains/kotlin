// FILE: 1.kt

inline fun <T> foo(x: Any) = x as T

inline fun <R> bar(x: Any): String {
    return foo<R>(x).toString()
}

// FILE: 2.kt

fun box(): String {
    return bar<Int>("OK")
}