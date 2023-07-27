// FILE: 1.kt

inline fun <T> foo(x: Any?): String {
    return object {
        fun bar(y: T) = y as String
    }.bar(x as T)
}

// FILE: 2.kt

fun box() = foo<Int>("OK")