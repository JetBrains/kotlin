// FILE: 1.kt

inline fun <T> foo(): String {
    var arr: Array<T> = Array(1) { Any() } as Array<T>
    arr[0] = bar()
    return "OK"
}

fun <T> bar(): T = "OK" as T

// FILE: 2.kt

fun box() = foo<Int>()
