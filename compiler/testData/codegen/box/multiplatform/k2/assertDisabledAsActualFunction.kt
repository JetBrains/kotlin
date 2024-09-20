// TARGET_BACKEND: NATIVE
// LANGUAGE: +MultiPlatformProjects
// ASSERTIONS_MODE: always-disable
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect fun foo()

fun box(): String {
    foo()
    return "OK"
}

// MODULE: native()()(common)
// FILE: native.kt
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)

actual fun foo() = assert(false)