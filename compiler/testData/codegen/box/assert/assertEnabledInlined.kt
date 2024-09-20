// TARGET_BACKEND: NATIVE
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

inline fun foo(x: Boolean) = assert(x)

fun box(): String {
    foo(true)
    return "OK"
}