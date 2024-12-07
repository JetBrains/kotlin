// TARGET_BACKEND: NATIVE
// LANGUAGE: +MultiPlatformProjects
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect fun foo(): String

fun box(): String = foo()

// MODULE: native()()(common)
// FILE: native.kt
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

actual fun foo(): String = try {
    assert(false)
    "FAIL"
} catch (e: AssertionError) {
    "OK"
}