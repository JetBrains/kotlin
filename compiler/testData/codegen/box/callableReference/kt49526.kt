// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// FILE: lib.kt

inline fun <T> useRef(value: T, f: (T) -> Boolean) = f(value)

// FILE: main.kt
fun box(): String {
    val chars = listOf('a') + "-"
    return if (useRef('a', chars::contains)) "OK" else "Failed"
}
