// FILE: 1.kt

inline fun <T> foo(x: Any?): String {
    val y: T = x as T
    return y as String
}

// FILE: 2.kt

fun box() = foo<Int>("OK")