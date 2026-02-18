// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt

inline fun <T> useRef(value: T, f: (T) -> Boolean) = f(value)

// FILE: main.kt
fun box(): String {
    val chars = listOf('a') + "-"
    val ref = chars::contains
    return if (ref('a')) "OK" else "Failed"
}
