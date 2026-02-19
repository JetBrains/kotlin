// TARGET_BACKEND: NATIVE
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

inline fun foo(x: Boolean) = assert(x)

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    foo(true)
    return "OK"
}