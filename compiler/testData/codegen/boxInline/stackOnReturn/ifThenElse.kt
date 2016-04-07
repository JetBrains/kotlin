// FILE: 1.kt
fun foo(x: Any?, y: Any?) = null

inline fun test(value: Any?): String? {
    return foo(null, if (value != null) value else return null)
}

// FILE: 2.kt
fun box(): String =
        test(null) ?: "OK"