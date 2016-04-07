// FILE: 1.kt
fun foo1(x: Long, xx: Int, y: Any?) = null

inline fun test1(value: Any?): String? {
    return foo1(0L, 0, value ?: return null)
}

// FILE: 2.kt
fun box(): String =
        test1(null) ?: "OK"