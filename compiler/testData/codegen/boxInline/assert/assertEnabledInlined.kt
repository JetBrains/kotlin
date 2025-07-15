// TARGET_BACKEND: NATIVE
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

// FILE: lib.kt
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

inline fun foo(x: Boolean) = assert(x)

// FILE: main.kt
fun box(): String {
    foo(true)
    return "OK"
}