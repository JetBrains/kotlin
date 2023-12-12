// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

interface I { fun ok(): String }
expect open class Base() { fun ok(): String }
class Child: Base(), I {}

fun box() = Base().ok()

// MODULE: platform()()(common)
// FILE: platform.kt

actual open class Base { actual fun ok() = "OK" }
