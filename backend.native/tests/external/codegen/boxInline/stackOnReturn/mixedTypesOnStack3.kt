// FILE: 1.kt
fun foo3(x: Int, xx: Long, xxx: Int, y: Any?) = null

inline fun test3(value: Any?): String? {
    return foo3(0, 0L, 0, value ?: return null)
}

// FILE: 2.kt
fun box(): String =
        test3(null) ?: "OK"