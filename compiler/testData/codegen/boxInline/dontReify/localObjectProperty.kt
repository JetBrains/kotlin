// FILE: 1.kt

inline fun <T> foo(x: Any?): String {
    return object {
        val y: T = x as T
    }.y as String
}

// FILE: 2.kt

fun box() = foo<Int>("OK")