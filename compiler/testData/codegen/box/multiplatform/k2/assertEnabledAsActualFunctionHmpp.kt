// TARGET_BACKEND: NATIVE
// LANGUAGE: +MultiPlatformProjects
// ASSERTIONS_MODE: always-enable
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect fun foo(): String

// MODULE: intermediate()()(common)
// FILE: intermediate.kt
fun box(): String = foo()

// MODULE: native()()(intermediate)
// FILE: native.kt
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

actual fun foo(): String = try {
    assert(false)
    "FAIL"
} catch (e: AssertionError) {
    "OK"
}